/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2020 Pierre Leresteux.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.saagie.technologies

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.kittinunf.fuel.Fuel
import com.saagie.technologies.model.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

class SaagieTechnologiesPackageGradlePlugin : Plugin<Project> {
    companion object {
        @JvmField
        val TIMEOUT_PUSH_PULL_DOCKER: Long = 10
        val metadataBaseFilename = "metadata"
        val technologyBaseFilename = "technology"
        val dockerInfoBaseFilename = "dockerInfo"
        val contextBaseFilename = "context"
        val dockerListing = "docker_listing"
        val outputDirectory = "tmp-zip"
    }

    override fun apply(project: Project) {
        /**
         * PACKAGE
         */

        val constructMetadata = constructMetadata(project)
        val packageAllVersionsForPromote = packageAllVersionsForPromote(project)
        val packageAllVersions = packageAllVersions(project, constructMetadata, packageAllVersionsForPromote)

        /**
         * PROMOTE
         */
        val metadataFileList = mutableListOf<String>()
        val downloadAndUnzipReleaseAssets = downloadAndUnzipReleaseAssets(project, metadataFileList)
        val fixMetadataVersion = fixMetadataVersion(project, downloadAndUnzipReleaseAssets, metadataFileList)
        val promote = promote(project, packageAllVersionsForPromote, fixMetadataVersion)
    }

    private fun promote(
        project: Project,
        packageAllVersionsForPromote: Task,
        fixMetadataVersion: Task
    ): Task = project.tasks.create("promote") {
        group = "technologies"
        description = "Promote the PR"

        doFirst {
            logger.info("> PROMOTE ${project.property("version")} DONE")
        }
        packageAllVersionsForPromote.mustRunAfter(fixMetadataVersion)
        dependsOn(fixMetadataVersion, packageAllVersionsForPromote)
    }

    private fun fixMetadataVersion(
        project: Project,
        downloadAndUnzipReleaseAssets: Task,
        metadataFileList: MutableList<String>
    ): Task = project.tasks.create("fixMetadataVersion") {
        dependsOn(downloadAndUnzipReleaseAssets)
        doFirst {
            val version = (project.property("version") as String)
            val dockerFormattedVersion = version.replace("+", "_")
            val newVersion = version.split("+").first()
            metadataFileList.forEach {
                val metadata = getJacksonObjectMapper()
                    .readValue((File(it)).inputStream(), ContextsMetadata::class.java)
                val tempFile = createTempFile()
                val file = File(it)
                tempFile.printWriter().use { writer ->
                    file.forEachLine { line ->
                        writer.println(
                            when {
                                line.startsWith("      version: ") && line.endsWith(dockerFormattedVersion)
                                -> line.replace("-$dockerFormattedVersion", "-$newVersion")
                                else -> line
                            }
                        )
                    }
                }
                metadata.contexts.forEach { context ->
                    if (context.dockerInfo?.version != null &&
                        context.dockerInfo.version.endsWith(dockerFormattedVersion)
                    ) {
                        logger.info("$it => ${context.dockerInfo.version}")
                        promoteDockerImage(context.dockerInfo)
                    }
                }
                tempFile.copyTo(file, true)
                logger.info("${file.path} UPDATED")
            }
            File("technologies")
                .walk()
                .filter { it.name == "$dockerInfoBaseFilename.yml" || it.name == "$dockerInfoBaseFilename.yaml" }
                .forEach { file ->
                    file.readText().let { line ->
                        file.writeText(line.replace("-$dockerFormattedVersion", "-$newVersion"))
                    }
                }
        }
    }

    private fun promoteDockerImage(metadataDocker: MetadataDocker) {
        with(
            DockerClientBuilder
                .getInstance(
                    DefaultDockerClientConfig
                        .createDefaultConfigBuilder()
                        .withRegistryUsername(System.getenv("DOCKER_USERNAME"))
                        .withRegistryPassword(System.getenv("DOCKER_PASSWORD"))
                        .build()
                )
                .build()
        ) {
            pullImageCmd(metadataDocker.imageSnapshot())
                .exec(PullImageResultCallback())
                .awaitCompletion(TIMEOUT_PUSH_PULL_DOCKER, TimeUnit.MINUTES)
            tagImageCmd(metadataDocker.imageSnapshot(), metadataDocker.image, metadataDocker.versionPromote())
                .exec()
            pushImageCmd(metadataDocker.imagePromote())
                .exec(PushImageResultCallback())
                .awaitCompletion(TIMEOUT_PUSH_PULL_DOCKER, TimeUnit.MINUTES)
        }
    }

    private fun downloadAndUnzipReleaseAssets(
        project: Project,
        metadataFileList: MutableList<String>
    ): Task = project.tasks.create("downloadAndUnzipReleaseAssets") {

        doFirst {
            this.project.checkEnvVar()
            val config = project.property("effectiveConfig") as ProjectConfigurationExtension
            val createTempFile = File.createTempFile("technologies", ".zip")
            val path = "${config.info.scm.url}/releases/download/" +
                    "${project.property("version")}/technologies.zip"
            logger.debug("Download assets : $path")
            Fuel.download(path)
                .fileDestination { _, _ -> createTempFile }
                .response()
            logger.info("Download OK => ${createTempFile.absolutePath}")
            ZipFile(createTempFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        if (
                            entry.name.endsWith("$metadataBaseFilename.yml") ||
                            entry.name.endsWith("$metadataBaseFilename.yaml")
                        ) {
                            logger.debug(">> ${entry.name}")
                            metadataFileList.add(entry.name)
                            File(entry.name).outputStream().use { input.copyTo(it) }
                        }
                    }
                }
            }
        }
    }

    private fun constructMetadata(project: Project): Task = project.tasks.create("constructMetadata") {
        doFirst {
            logger.info("Construct metadata")
            File(project.rootDir.path + "/technologies").walkTopDown().forEach {
                when {
                    it.isADirectoryContainingFile(technologyBaseFilename) -> {
                        val targetMetadata = File("$it/$metadataBaseFilename.yaml")
                        targetMetadata.delete()
                        File("$it/$technologyBaseFilename.yaml").checkYamlExtension().copyTo(targetMetadata)
                        targetMetadata.appendText("\ncontexts:")
                        it.walkTopDown().forEach {
                            when {
                                it.isADirectoryContainingFile(contextBaseFilename) -> {
                                    val dockerInfoFile = File("$it/$dockerInfoBaseFilename.yaml").checkYamlExtension()
                                    val dockerVersion = when {
                                        dockerInfoFile.exists() -> getJacksonObjectMapper()
                                            .readValue(
                                                dockerInfoFile.inputStream(),
                                                ContextMetadata::class.java
                                            ).dockerInfo?.version
                                        else -> null
                                    }
                                    File("$it/$contextBaseFilename.yaml")
                                        .checkYamlExtension()
                                        .readLines()
                                        .forEachIndexed { index, line ->
                                            when (index) {
                                                0 -> targetMetadata.appendText("\n  - $line")
                                                else -> targetMetadata.appendText("\n    $line")
                                            }
                                            if (line.startsWith("  image:") && dockerVersion != null) {
                                                targetMetadata.appendText("\n      version: $dockerVersion")
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun packageAllVersionsForPromote(
        project: Project
    ): Task = project.tasks.create("packageAllVersionsForPromote") {
        doFirst {
            with("${project.rootDir.path}/$outputDirectory/") {
                val rootZipDir = File(this)
                rootZipDir.deleteRecursively()
                rootZipDir.mkdir()
                var hasVersion = false
                File(project.rootDir.path + "/technologies").walkTopDown().forEach {
                    when {
                        it.isADirectoryContainingFile(metadataBaseFilename) -> {
                            logger.info("VERSION : ${project.relativePath(it.toPath())}")
                            hasVersion = true
                            File("${rootZipDir.absolutePath}/${project.relativePath(it.toPath())}").mkdir()

                            File("${project.relativePath(it.toPath())}/$metadataBaseFilename.yaml")
                                .checkYamlExtension()
                                .copyTo(
                                    File("$this/${project.relativePath(it.toPath())}/$metadataBaseFilename.yaml")
                                )
                        }
                    }
                }
                if (hasVersion) {
                    project.exec {
                        workingDir = rootZipDir
                        executable = "zip"
                        args = listOf(
                            "-r",
                            "technologies.zip",
                            "technologies"
                        )
                    }
                    generateListing(this)
                }
            }
        }
    }

    private fun getJacksonYamlObjectMapper(): ObjectMapper =
        ObjectMapper(
            YAMLFactory()
                .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
                .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
        ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(KotlinModule()).setSerializationInclusion(JsonInclude.Include.NON_NULL)

    private fun getJacksonJsonObjectMapper(): ObjectMapper =
        ObjectMapper(JsonFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .registerModule(KotlinModule()).setSerializationInclusion(JsonInclude.Include.NON_NULL)

    private fun generateListing(path: String) {
        val yamlObjectMapper = getJacksonYamlObjectMapper()
        val jsonObjectMapper = getJacksonJsonObjectMapper()
        val dockerImages: MutableList<String> = mutableListOf()
        val listing = File(path)
            .walk()
            .filter {
                it.name == "$metadataBaseFilename.yml" ||
                        it.name == "$metadataBaseFilename.yaml"
            }
            .map {
                yamlObjectMapper.readValue(it, SimpleMetadataWithContexts::class.java)
            }
            .map { it.toListing() }
            .map { techno ->
                when {
                    techno.docker != null -> {
                        dockerImages.add(techno.docker)
                    }
                    else -> {
                        techno.contexts?.forEach { context ->
                            if (context.docker != null) {
                                dockerImages.add(context.docker)
                            }
                        }
                    }
                }
                techno
            }
        jsonObjectMapper.writeValue(File(path + "/$dockerListing.json"), listing)

        with(File(path + "/$dockerListing.txt")) {
            dockerImages.forEach {
                this.appendText(it + "\n")
            }
        }
    }

    private fun packageAllVersions(
        project: Project,
        packageAllVersionsForPromote: Task,
        constructMetadata: Task
    ): Task = project.tasks.create("packageAllVersions") {
        group = "technologies"
        description = "Package all versions"
        packageAllVersionsForPromote.mustRunAfter(constructMetadata)
        dependsOn(constructMetadata, packageAllVersionsForPromote)
    }
}

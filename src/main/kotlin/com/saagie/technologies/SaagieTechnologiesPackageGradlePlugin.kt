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

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.PullImageResultCallback
import com.github.dockerjava.core.command.PushImageResultCallback
import com.github.kittinunf.fuel.Fuel
import com.saagie.technologies.model.ContextMetadata
import com.saagie.technologies.model.ContextsMetadata
import com.saagie.technologies.model.MetadataDocker
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
    }

    override fun apply(project: Project) {
        /**
         * PACKAGE
         */
        val outputDirectory = "tmp-zip"
        val metadataFilename = "metadata.yml"

        val packageAllVersions = packageAllVersions(project, outputDirectory, metadataFilename)

        /**
         * PROMOTE
         */
        val metadataFileList = mutableListOf<String>()
        val downloadAndUnzipReleaseAssets = downloadAndUnzipReleaseAssets(project, metadataFileList)
        val fixMetadataVersion = fixMetadataVersion(project, downloadAndUnzipReleaseAssets, metadataFileList)
        val promote = promote(project, packageAllVersions, fixMetadataVersion)
    }

    private fun promote(
        project: Project,
        packageAllVersions: Task,
        fixMetadataVersion: Task
    ): Task = project.tasks.create("promote") {
        group = "technologies"
        description = "Promote the PR"

        doFirst {
            logger.info("> PROMOTE ${project.property("version")} DONE")
        }
        packageAllVersions.mustRunAfter(fixMetadataVersion)
        dependsOn(fixMetadataVersion, packageAllVersions)
    }

    private fun fixMetadataVersion(
        project: Project,
        downloadAndUnzipReleaseAssets: Task,
        metadataFileList: MutableList<String>
    ): Task = project.tasks.create("fixMetadataVersion") {
        dependsOn(downloadAndUnzipReleaseAssets)
        doFirst {
            metadataFileList.forEach {
                val metadata = getJacksonObjectMapper()
                    .readValue((File(it)).inputStream(), ContextsMetadata::class.java)
                val tempFile = createTempFile()
                val file = File(it)
                metadata.contexts.forEach { context ->
                    if (context.dockerInfo?.version != null &&
                        context.dockerInfo.version.endsWith(project.property("version") as String)
                    ) {
                        logger.debug("$it => ${context.dockerInfo.version}")
                        tempFile.printWriter().use { writer ->
                            file.forEachLine { line ->
                                writer.println(
                                    when {
                                        line.startsWith("    version: ") &&
                                            line.endsWith(context.dockerInfo.version)
                                        -> {
                                            line.replace(
                                                context.dockerInfo.version,
                                                context.dockerInfo.version.split("_").first()
                                            )
                                        }
                                        else -> line
                                    }
                                )
                            }
                        }
                        promoteDockerImage(context.dockerInfo)
                    }
                    tempFile.copyTo(file, true)
                    logger.info("${file.path} UPDATED")
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
                        if (entry.name.endsWith("metadata.yml")) {
                            logger.debug(">> ${entry.name}")
                            metadataFileList.add(entry.name)
                            File(entry.name).outputStream().use { input.copyTo(it) }
                        }
                    }
                }
            }
        }
    }

    private fun constructMetadata(project: Project) {
        File(project.rootDir.path + "/technologies").walkTopDown().forEach {
            when {
                it.isADirectoryContainingFile("techno.yml") -> {
                    val targetMetadata = File("$it/metadata.yml")
                    targetMetadata.delete()
                    File("$it/techno.yml").copyTo(targetMetadata)
                    targetMetadata.appendText("contexts:")
                    it.walkTopDown().forEach {
                        when {
                            it.isADirectoryContainingFile("context.yml") -> {
                                val dockerVersion = getJacksonObjectMapper()
                                    .readValue((it).inputStream(), ContextMetadata::class.java).dockerInfo?.version
                                File("$it/context.yml").forEachLine {
                                    targetMetadata.appendText("  $it")
                                    if (it.startsWith("    image:") && dockerVersion != null) {
                                        targetMetadata.appendText("  version: $dockerVersion")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun packageAllVersions(
        project: Project,
        outputDirectory: String,
        metadataFilename: String
    ): Task = project.tasks.create("packageAllVersions") {
        group = "technologies"
        description = "Package all versions"
        doFirst {
            constructMetadata(project)
            with("${project.rootDir.path}/$outputDirectory/") {
                val rootZipDir = File(this)
                rootZipDir.deleteRecursively()
                rootZipDir.mkdir()
                var hasVersion = false
                File(project.rootDir.path + "/technologies").walkTopDown().forEach {
                    when {
                        it.isADirectoryContainingFile(metadataFilename) -> {
                            logger.info("VERSION : ${project.relativePath(it.toPath())}")
                            hasVersion = true
                            File("${rootZipDir.absolutePath}/${project.relativePath(it.toPath())}").mkdir()

                            File("${project.relativePath(it.toPath())}/$metadataFilename").copyTo(
                                File("$this/${project.relativePath(it.toPath())}/$metadataFilename")
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
                }
            }
        }
    }
}

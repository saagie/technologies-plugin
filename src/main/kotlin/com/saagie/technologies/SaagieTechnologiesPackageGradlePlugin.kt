/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Pierre Leresteux.
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

import com.github.kittinunf.fuel.Fuel
import com.saagie.technologies.model.Metadata
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import java.io.File
import java.util.zip.ZipFile

class SaagieTechnologiesPackageGradlePlugin : Plugin<Project> {
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

        val fixMetadataVersion =
            fixMetadataVersion(project, downloadAndUnzipReleaseAssets, metadataFileList)

        val promote = promote(project, packageAllVersions, fixMetadataVersion)
    }

    private fun promote(
        project: Project,
        packageAllVersions: Task,
        fixMetadataVersion: Task
    ): Task {
        return project.tasks.create("promote") {
            group = "technologies"
            description = "Promote the PR"

            doFirst {
                logger.info("> PROMOTE ${project.property("version")} DONE")
            }
            packageAllVersions.mustRunAfter(fixMetadataVersion)
            dependsOn(fixMetadataVersion, packageAllVersions)
        }
    }

    private fun fixMetadataVersion(
        project: Project,
        downloadAndUnzipReleaseAssets: Task,
        metadataFileList: MutableList<String>
    ): Task {
        return project.tasks.create("fixMetadataVersion") {
            dependsOn(downloadAndUnzipReleaseAssets)
            doFirst {
                metadataFileList.forEach {
                    val metadata = getJacksonObjectMapper()
                        .readValue((File(it)).inputStream(), Metadata::class.java)
                    if (metadata.techno.docker.version != null &&
                        metadata.techno.docker.version.endsWith(project.property("version") as String)) {
                        logger.debug("$it => ${metadata.techno.docker.version}")
                        val tempFile = createTempFile()
                        val file = File(it)
                        tempFile.printWriter().use { writer ->
                            file.forEachLine { line ->
                                writer.println(
                                    when {
                                        line.startsWith("    version: ") &&
                                                line.endsWith(metadata.techno.docker.version)
                                        -> {
                                            line.replace(
                                                metadata.techno.docker.version,
                                                metadata.techno.docker.version.split("_").first()
                                            )
                                        }
                                        else -> line
                                    }
                                )
                            }
                        }
                        tempFile.copyTo(file, true)
                        logger.info("${file.path} UPDATED")
                        // pushDocker()
                    }
                }
            }
        }
    }

    private fun downloadAndUnzipReleaseAssets(
        project: Project,
        metadataFileList: MutableList<String>
    ): Task {
        return project.tasks.create("downloadAndUnzipReleaseAssets") {
            val createTempFile = File.createTempFile("certified", ".zip")

            doFirst {
                val config = project.property("effectiveConfig") as ProjectConfigurationExtension
                val path = "${config.info.scm.url}/releases/download/${project.property("version")}/certified.zip"
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
    }

    private fun packageAllVersions(
        project: Project,
        outputDirectory: String,
        metadataFilename: String
    ): Task {
        return project.tasks.create("packageAllVersions") {
            group = "technologies"
            description = "Package all versions"

            doFirst {
                with("${project.rootDir.path}/$outputDirectory/") {
                    val rootZipDir = File(this)
                    rootZipDir.deleteRecursively()
                    rootZipDir.mkdir()

                    SCOPE.values().map { scope ->
                        var hasVersion = false
                        File(project.rootDir.path + "/" + scope.folderName).walkTopDown().forEach {
                            when {
                                it.isAVersion() -> {
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
                                    "${scope.folderName}.zip",
                                    "${scope.folderName}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

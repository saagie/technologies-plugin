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

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project

class SaagieTechnologiesPackageGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        /**
         * PACKAGE
         */
        val outputDirectory = "tmp-zip"
        val metadataFilename = "metadata.yml"

        val packageAllVersions = project.tasks.create("packageAllVersions") {
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
                                    logger.error("VERSION : ${project.relativePath(it.toPath())}")
                                    hasVersion = true
                                    File("${rootZipDir.absolutePath}/${project.relativePath(it.toPath())}").mkdir()

                                    File("${project.relativePath(it.toPath())}/$metadataFilename").copyTo(
                                        File(
                                            "$this/${project.relativePath(
                                                it.toPath()
                                            )}/$metadataFilename"
                                        )
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
                                    "${rootZipDir.absolutePath}/${scope.folderName}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

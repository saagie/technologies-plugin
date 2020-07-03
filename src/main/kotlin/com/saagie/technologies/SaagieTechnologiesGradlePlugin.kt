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

import com.bmuschko.gradle.docker.tasks.container.*
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import java.io.File
import java.util.*

class SaagieTechnologiesGradlePlugin : Plugin<Project> {
    companion object {
        @Suppress("MayBeConst")
        @JvmField
        val TIMEOUT_TEST_CONTAINER = 10
    }

    override fun apply(project: Project) {

        /**
         * BUILD IMAGES
         */
        val dockerInfo = readDockerInfo(project.projectDir)
        val imageName = project.generateDockerTag(dockerInfo)

        val imageTestName = "gcr.io/gcp-runtimes/container-structure-test:latest"
        var logs = ""

        val buildImage = project.tasks.create<DockerBuildImage>("buildImage") {
            doFirst {
                if (project.projectDir.absoluteFile.toPath().toString().contains("/innerContexts/")) {
                    File("${project.projectDir.parent}/Dockerfile").copyTo(File("${project.projectDir}/Dockerfile"))
                    if (File("${project.projectDir.parent}/entrypoint.sh").exists()) {
                        File("${project.projectDir.parent}/entrypoint.sh").copyTo(File("${project.projectDir}/entrypoint.sh"))
                    }
                }
            }
            this.noCache.set(true)
            this.inputDir.set(File("."))
            this.images.add(imageName)
            doLast {
                if (project.projectDir.absoluteFile.toPath().toString().contains("/innerContexts/")) {
                    File("${project.projectDir}/Dockerfile").delete()
                    if (File("${project.projectDir}/entrypoint.sh").exists()) {
                        File("${project.projectDir}/entrypoint.sh").delete()
                    }
                }
            }
        }

        val pullDockerImage = project.tasks.create<DockerPullImage>("pullDockerImage") {
            image.set(imageTestName)
        }

        val createContainer = project.tasks.create<DockerCreateContainer>("createContainer") {
            dependsOn(pullDockerImage)
            doFirst {
                if (project.projectDir.absoluteFile.toPath().toString().contains("/innerContexts/")) {
                    File("${project.projectDir.parent}/image_test.yaml").copyTo(File("${project.projectDir}/image_test.yaml"))
                }
            }
            targetImageId(imageTestName)
            hostConfig.autoRemove.set(false)
            hostConfig.binds.put("/var/run/docker.sock", "/var/run/docker.sock")
            workingDir.set("/workdir")
            val imageTestFile = "${project.projectDir.absolutePath}/image_test".checkYamlExtension()
            hostConfig.binds.put(imageTestFile, "/workdir/image_test.yaml")
            cmd.addAll("test", "--image", imageName, "--config", "/workdir/image_test.yaml")
            doLast {
                if (project.projectDir.absoluteFile.toPath().toString().contains("/innerContexts/")) {
                    File("${project.projectDir}/image_test.yaml").delete()
                }
            }
        }

        val startContainer = project.tasks.create<DockerStartContainer>("startContainer") {
            dependsOn(createContainer)
            targetContainerId(createContainer.containerId)
        }

        val removeContainer = project.tasks.create<DockerRemoveContainer>("removeContainer") {
            force.set(true)
            targetContainerId(createContainer.containerId)
        }

        val logContainer = project.tasks.create<DockerLogsContainer>("logContainer") {
            dependsOn(startContainer)
            targetContainerId(createContainer.containerId)
            follow.set(true)
            tailAll.set(true)
            onNext {
                logs += "$this \n"
            }
        }
        val buildWaitContainer = project.tasks.create<DockerWaitContainer>("buildWaitContainer") {
            dependsOn(logContainer)
            targetContainerId(createContainer.containerId)
            awaitStatusTimeout.set(TIMEOUT_TEST_CONTAINER)
            doLast {
                if (exitCode != 0) {
                    logger.error(logs)
                    throw GradleException("Tests on ${project.name} failed")
                }
                logger.info(" *** TESTS SUCCESFULL ***")
            }
            finalizedBy(removeContainer)
        }

        val testImage = project.tasks.create("testImage") {
            dependsOn(buildImage, buildWaitContainer)
            startContainer.mustRunAfter(buildImage)
        }

        val testNoBuildImage = project.tasks.create("testNoBuildImage") {
            dependsOn(buildWaitContainer)
            startContainer.mustRunAfter(buildImage)
        }

        val pushImage = project.tasks.create<DockerPushImage>("pushImage") {
            doFirst {
                checkEnvVar()
            }
            dependsOn(testImage)
            this.images.add(imageName)
            this.registryCredentials {
                username.set(System.getenv("DOCKER_USERNAME"))
                password.set(System.getenv("DOCKER_PASSWORD"))
            }
        }

        val generateDockerInfo = project.tasks.create("generateDockerInfo") {
            dependsOn(pushImage)
            doLast {
                storeDockerInfo(project, dockerInfo)
            }
        }

        val buildDockerImage = project.tasks.create("buildDockerImage") {
            group = "technologies"
            description = "Build techno"
            dependsOn(generateDockerInfo)
        }
    }
}

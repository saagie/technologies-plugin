/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2022 Creative Data.
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

import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerLogsContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerWaitContainer
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.github.gradle.node.yarn.task.YarnTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.create
import java.io.File

class SaagieTechnologiesGradlePlugin : Plugin<Project> {
    companion object {
        @Suppress("MayBeConst")
        @JvmField
        val TIMEOUT_TEST_CONTAINER = 10
    }

    override fun apply(project: Project) {
        val buildTechnology = project.tasks.create("buildTechnology") {
            group = "technologies"
            description = "Build the technology"
        }

        val testTechnology = project.tasks.create("testTechnology") {
            group = "technologies"
            description = "Build and test the technology"
        }

        if (project.isDockerModule()) {
            applyDocker(project, buildTechnology, testTechnology)
        }
        if (project.isJsModule()) {
            applyJs(project, buildTechnology, testTechnology)
        }
    }

    private fun applyDocker(project: Project, buildTechnology: Task, testTechnology: Task) {

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
                    File("${project.projectDir.parent}/Dockerfile").copyTo(File("${project.projectDir}/Dockerfile"), true)
                    if (File("${project.projectDir.parent}/entrypoint.sh").exists()) {
                        File("${project.projectDir.parent}/entrypoint.sh").copyTo(File("${project.projectDir}/entrypoint.sh"), true)
                    }
                }
            }
            this.noCache.set(true)
            this.inputDir.set(File("."))
            this.images.add(imageName)
            this.registryCredentials {
                username.set(System.getenv("DOCKER_USERNAME"))
                password.set(System.getenv("DOCKER_PASSWORD"))
            }
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
            targetImageId(imageTestName)
            hostConfig.autoRemove.set(false)
            hostConfig.binds.put("/var/run/docker.sock", "/var/run/docker.sock")
            workingDir.set("/workdir")
            val imageTestFile = "${project.projectDir.absolutePath}/image_test".checkYamlExtension()
            hostConfig.binds.put(imageTestFile, "/workdir/image_test.yaml")
            cmd.addAll("test", "--image", imageName, "--config", "/workdir/image_test.yaml")
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
            description = "Build docker based technology"
            dependsOn(generateDockerInfo)
        }

        buildTechnology.dependsOn(buildDockerImage)
        testTechnology.dependsOn(testImage)
    }

    private fun applyJs(project: Project, buildTechnology: Task, testTechnology: Task) {

        val yarnBuild = project.tasks.create<YarnTask>("yarnBuild") {
            dependsOn("yarn_install") // this task should be auto generated by the node gradle plugin
            args.set(listOf("build"))
        }

        val yarnTest = project.tasks.create<YarnTask>("yarnTest") {
            dependsOn(yarnBuild)
            args.set(listOf("test"))
        }

        val buildExternalJsScript = project.tasks.create("buildExternalJsScript") {
            group = "technologies"
            description = "Build external js"
            dependsOn("yarnBuild")
        }

        buildTechnology.dependsOn(buildExternalJsScript)
        testTechnology.dependsOn(yarnTest)
    }
}

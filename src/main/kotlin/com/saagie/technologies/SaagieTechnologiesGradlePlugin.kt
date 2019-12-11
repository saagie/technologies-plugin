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

import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerLogsContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerWaitContainer
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class SaagieTechnologiesGradlePlugin : Plugin<Project> {
    companion object {
        @JvmField
        val TIMEOUT_TEST_CONTAINER = 10
    }

    override fun apply(project: Project) {

        /**
         * BUILD IMAGES
         */

        val metadata = readMetadata(project.projectDir)
        val imageName = generateDockerTag(project, metadata)

        val imageTestNameDetails = Pair("gcr.io/gcp-runtimes/container-structure-test", "latest")
        val imageTestName = "${imageTestNameDetails.first}:${imageTestNameDetails.second}"
        var logs = ""

        val buildImage = project.tasks.create<DockerBuildImage>("buildImage") {
            this.inputDir.set(File("."))
            this.tags.add(imageName)
        }

        val pullDockerImage = project.tasks.create<DockerPullImage>("pullDockerImage") {
            repository.set(imageTestNameDetails.first)
            tag.set(imageTestNameDetails.second)
        }

        val createContainer = project.tasks.create<DockerCreateContainer>("createContainer") {
            dependsOn(pullDockerImage)
            targetImageId(imageTestName)
            autoRemove.set(false)
            binds.put("${project.projectDir.absolutePath}/image_test.yml", "/workdir/image_test.yml")
            binds.put("/var/run/docker.sock", "/var/run/docker.sock")
            workingDir.set("/workdir")
            cmd.addAll("test", "--image", imageName, "--config", "/workdir/image_test.yml")
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
            }
            finalizedBy(removeContainer)
        }

        val testImage = project.tasks.create("testImage") {
            dependsOn(buildImage, buildWaitContainer)
            startContainer.mustRunAfter(buildImage)
        }

        val pushImage = project.tasks.create<DockerPushImage>("pushImage") {
            dependsOn(testImage)
            this.imageName.set(imageTestNameDetails.first)
            this.tag.set(imageTestNameDetails.second)
        }

        val generateMetadata = project.tasks.create("generateMetadata") {
            dependsOn(pushImage)
            doLast {
                storeMetadata(project, project.projectDir, metadata)
            }
        }

        val buildDockerImage = project.tasks.create("buildDockerImage") {
            group = "technologies"
            description = "Build techno"
            dependsOn(generateMetadata)
        }
    }
}

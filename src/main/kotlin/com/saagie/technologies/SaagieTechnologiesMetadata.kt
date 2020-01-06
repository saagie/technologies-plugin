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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.saagie.technologies.model.Metadata
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File
import java.util.Optional

fun generateDockerTag(project: Project, metadata: Metadata) =
    "${metadata.techno.docker.image}:${project.generateTag()}"

fun storeMetadata(project: Project, projectDir: File, metadata: Metadata) {
    val targetMetadata = File("${projectDir.absolutePath}/metadata.yml")
    targetMetadata.delete()
    File("${projectDir.absolutePath}/version.yml").copyTo(targetMetadata)
    targetMetadata.appendText(
        "\n" +
                getJacksonObjectMapper().writeValueAsString(
                    metadata.copy(
                        metadata.techno.copy(
                            docker = metadata.techno.docker.copy(version = project.generateTag())
                        )
                    )
                )
    )
}

fun readMetadata(projectDir: File): Metadata =
    getJacksonObjectMapper().readValue(
        File("${projectDir.parentFile.absoluteFile}/techno.yml").inputStream(),
        Metadata::class.java
    )

fun getJacksonObjectMapper(): ObjectMapper =
    ObjectMapper(
        YAMLFactory()
            .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
            .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
    ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule()).setSerializationInclusion(JsonInclude.Include.NON_NULL)

fun Project.generateTag(): String = "${this.name}-${this.getVersionForDocker()}"
fun Project.getVersionForDocker(): String = "${this.rootProject.version}".replace("+", "_")
fun Project.checkEnvVar() {
    listOf("DOCKER_USERNAME", "DOCKER_PASSWORD").forEach {
        if (!Optional.ofNullable(System.getenv(it)).isPresent) {
            throw GradleException("ENV $it is not set")
        }
    }
}

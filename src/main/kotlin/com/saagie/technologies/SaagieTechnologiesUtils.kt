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

import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File

enum class TYPE(val folderName: String) {
    JOB("job"),
    APP("app")
}
enum class SCOPE(val folderName: String) {
    CERTIFIED("certified"),
    EXPERIMENTAL("experimental")
}

fun File.isAVersion(): Boolean = this.isDirectory && File(this.absolutePath + "/metadata.yml").exists()
fun String.isA(type: TYPE): Boolean = this.contains("/" + type.folderName + "/")

fun modifiedProjects(type: TYPE, subProjects: MutableSet<Project>): Set<Project> {
    val listModifiedProjects = mutableSetOf<Project>()

    Runtime.getRuntime().exec(
        arrayOf(
            "bash",
            "-c",
            "git diff --name-only  origin/master..."
        )
    ).inputStream.bufferedReader().readText()
        .split("\n").dropLast(1)
        .forEach { pathFile ->
            if (pathFile.isA(type) && File(pathFile).isFile) {
                subProjects
                    .filter { it.path == ":${pathFile.split("/").dropLast(1).last()}" }
                    .forEach { listModifiedProjects.add(it) }
            }
        }
    return listModifiedProjects
}
fun getCurrentBranchName(project: Project): String {
    ByteArrayOutputStream().apply {
        val os = this
        project.exec {
            executable = "git"
            args = listOf("rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = os
        }
        return os.toString().trim()
    }
}

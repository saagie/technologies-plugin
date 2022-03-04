/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021.
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
import java.io.File

enum class TYPE(val folderName: String) {
    JOB("job"),
    APP("app"),
    CONNECTION_TYPE("connectiontype")
}

fun File.isADirectoryContainingFile(fileName: String): Boolean =
    this.isDirectory &&
        (
            File(this.absolutePath + "/$fileName.yaml").exists() ||
                File(this.absolutePath + "/$fileName.yml").exists()
            )

fun String.isA(type: TYPE): Boolean = this.contains("/" + type.folderName + "/")

fun modifiedProjects(type: TYPE, subProjects: MutableSet<Project>): Set<Project> {
    val listModifiedProjects = mutableSetOf<Project>()

    Runtime.getRuntime().exec(
        arrayOf(
            "bash",
            "-c",
            "git diff --name-only origin/master"
        )
    ).inputStream.bufferedReader().readText()
        .split("\n").dropLast(1)
        .filter { pathFile ->
            !pathFile.contains("context.yaml", true) &&
                !pathFile.contains("technology.yaml", true) &&
                !pathFile.contains("innerContext.yaml", true) &&
                !pathFile.contains("metadata.yaml", true)
        } // Do not rebuild if metadata files are the only ones modified
        .forEach { pathFile ->
            if (pathFile.isA(type) && (File(pathFile).isFile || !File(pathFile).exists())) {
                subProjects
                    .filter { it.path == ":${pathFile.split("/").dropLast(1).last()}" }
                    .forEach { listModifiedProjects.add(it) }
            }
        }
    return listModifiedProjects
}

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
package com.saagie.technologies.model

data class DockerInfo(
    val image: String,
    val baseTag: String,
    val dynamicVersion: String? = null,
    val version: String = "$baseTag-$dynamicVersion"
) {
    constructor() : this("", "")

    fun generateDocker() = "$image:$baseTag-$dynamicVersion".removeIllegalDockerCharacters()
    fun generateDockerPromote() = "$image:$baseTag-${promoteVersion()}".removeIllegalDockerCharacters()
    fun promoteVersion() = "$baseTag-${dynamicVersion?.removeBranchName()}"
}

private fun String.removeIllegalDockerCharacters() = this.replace("+", "_")

private fun String.removeBranchName() = this.split("_")?.first()

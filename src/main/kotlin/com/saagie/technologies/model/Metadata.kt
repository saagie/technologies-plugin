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

data class Metadata(
    val techno: MetadataTechno
) {
    constructor() : this(MetadataTechno())
}

data class MetadataTechno(
    val id: String,
    val label: String,
    val available: Boolean,
    val minimumProductVersion: String?,
    val icon: String,
    val recommendedVersion: String?,
    val docker: MetadataDocker?
) {
    constructor() : this("", "", false, null, "", null, null)
}

data class MetadataDocker(
    val image: String,
    val version: String? = null
) {
    constructor() : this("", "")
    fun imageSnapshot() = "$image:$version"
    fun imagePromote() = "$image:${versionPromote()}"
    fun versionPromote() = version?.split("_")?.first()
}

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

data class ContextMetadata(
    val dockerInfo: MetadataDocker?
) {
    constructor() : this(null)
}

data class ContextMetadataWithId(
    val id: String?,
    val dockerInfo: MetadataDocker?
) {
    constructor() : this(null, null)
}

data class ContextsMetadata(
    val contexts: List<ContextMetadata> = emptyList()
) {
    constructor() : this(emptyList())
}

data class SimpleMetadataWithContexts(
    val id: String,
    val type: String,
    val dockerInfo: MetadataDocker?,
    val contexts: List<ContextMetadataWithId>?
) {
    constructor() : this("", "", null, null)
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

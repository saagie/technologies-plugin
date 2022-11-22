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
package com.saagie.technologies.model

data class FinalContextMetadata(
    val available: Boolean?,
    val dockerInfo: DockerInfo?
) {
    constructor() : this(true, null)
}

data class InnerContextMetadata(
    val available: Boolean?,
    val dockerInfo: DockerInfo?,
    val innerContexts: List<FinalContextMetadata>?
) {
    constructor() : this(true, null, emptyList())
}

data class ContextMetadata(
    val available: Boolean?,
    val dockerInfo: DockerInfo?,
    val innerContexts: List<InnerContextMetadata>?
) {
    constructor() : this(true, null, emptyList())
}

data class ContextsMetadata(
    val contexts: List<ContextMetadata> = emptyList()
) {
    constructor() : this(emptyList())
}

data class FinalContextMetadataWithId(
    val id: String?,
    val available: Boolean?,
    val dockerInfo: DockerInfo?
) {
    constructor() : this(null, true, null)
}

data class InnerContextMetadataWithId(
    val id: String?,
    val available: Boolean?,
    val dockerInfo: DockerInfo?,
    val innerContexts: List<FinalContextMetadataWithId>?

) {
    constructor() : this(null, true, null, emptyList())
}

data class ContextMetadataWithId(
    val id: String?,
    val available: Boolean?,
    val dockerInfo: DockerInfo?,
    val innerContexts: List<InnerContextMetadataWithId>?
) {
    constructor() : this(null, true, null, emptyList())
}

data class SimpleMetadataWithContexts(
    val id: String,
    val type: String,
    val available: Boolean?,
    val dockerInfo: DockerInfo?,
    val contexts: List<ContextMetadataWithId>?
) {
    constructor() : this("", "", true, null, emptyList())
}

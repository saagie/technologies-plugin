/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019-2021 Pierre Leresteux.
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

data class Listing(
    val technoId: String,
    val technoType: String,
    val docker: String?,
    val contexts: List<ContextListing>?
) {
    constructor() : this("", "", null, emptyList())
}

data class ContextListing(
    val id: String,
    val docker: String?,
    val innerContexts: List<InnerContextListing>?
) {
    constructor() : this("", null, emptyList())
}

data class InnerContextListing(
    val id: String,
    val innerContexts: List<FinalContextListing>?
) {
    constructor() : this("", emptyList())
}

data class FinalContextListing(
    val id: String,
    val docker: String?
) {
    constructor() : this("", null)
}

fun ContextMetadataWithId.toContextListing() = ContextListing(
    id = this.id ?: "",
    docker = this.dockerInfo.toOneLine(),
    innerContexts = this.innerContexts?.map { it.toInnerContextListing() }
)

fun InnerContextMetadataWithId.toInnerContextListing() = InnerContextListing(
        id = this.id ?: "",
        innerContexts = this.innerContexts?.map { it.toFinalContextListing() }
)

fun FinalContextMetadataWithId.toFinalContextListing() = FinalContextListing(
        id = this.id ?: "",
        docker = this.dockerInfo.toOneLine()
)

fun DockerInfo?.toOneLine() = when {
    this != null -> "$image:$version"
    else -> null
}

fun SimpleMetadataWithContexts.toDocker(): String? =
    when (this.type) {
        "APP" -> this.dockerInfo.toOneLine()
        else -> null
    }

fun SimpleMetadataWithContexts.toListing() = Listing(
    technoId = this.id,
    technoType = this.type,
    docker = this.toDocker(),
    contexts = this.contexts?.map { it.toContextListing() })

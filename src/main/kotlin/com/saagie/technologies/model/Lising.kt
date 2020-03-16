package com.saagie.technologies.model

data class Listing(
    val technoId: String,
    val technoType: String,
    val docker: String?,
    val contexts: List<ContextListing>?
) {
    constructor() : this("", "", null, null)
}

data class ContextListing(
    val id: String,
    val docker: String?
) {
    constructor() : this("", null)
}

fun ContextMetadataWithId.toContextListing() = ContextListing(
    id = this.id ?: "",
    docker = this.dockerInfo.toOneLine()
)

fun MetadataDocker?.toOneLine() = when {
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

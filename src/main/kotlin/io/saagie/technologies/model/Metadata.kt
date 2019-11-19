package io.saagie.technologies.model

data class Metadata(
    val techno: MetadataTechno
) {
    constructor() : this(MetadataTechno())
}

data class MetadataTechno(
    val id: String,
    val label: String,
    val isAvailable: Boolean,
    val minimumProductVersion: String?,
    val icon: String,
    val recommendedVersion: String,
    val docker: MetadataDocker
) {
    constructor() : this("", "", false, "", "", "", MetadataDocker())
}

data class MetadataDocker(
    val image: String,
    val version: String? = null
) {
    constructor() : this("", "")
}

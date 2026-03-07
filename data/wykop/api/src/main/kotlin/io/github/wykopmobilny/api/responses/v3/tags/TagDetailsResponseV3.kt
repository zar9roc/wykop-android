package io.github.wykopmobilny.api.responses.v3.tags

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDetailsResponseV3(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "personal") val personal: Boolean?,
    @field:Json(name = "media") val media: TagMediaResponseV3?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "followers") val followers: Int?,
    @field:Json(name = "follow") val follow: Boolean?,
    @field:Json(name = "blacklist") val blacklist: Boolean?,
    @field:Json(name = "editable") val editable: Boolean?,
)

@JsonClass(generateAdapter = true)
data class TagMediaResponseV3(
    @field:Json(name = "photo") val photo: TagPhotoResponseV3?,
)

@JsonClass(generateAdapter = true)
data class TagPhotoResponseV3(
    @field:Json(name = "url") val url: String?,
)

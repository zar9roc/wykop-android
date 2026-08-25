package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// GET /v3/links/draft/{key} - dane zescrapowanego draftu.
@JsonClass(generateAdapter = true)
data class LinkDraftResponseV3(
    @field:Json(name = "key") val key: String,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "images") val images: List<LinkDraftImageResponseV3>? = null,
    @field:Json(name = "adult") val adult: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class LinkDraftImageResponseV3(
    @field:Json(name = "url") val url: String,
    @field:Json(name = "selected") val selected: Boolean? = null,
)

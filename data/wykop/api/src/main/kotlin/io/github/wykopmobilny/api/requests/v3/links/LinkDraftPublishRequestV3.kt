package io.github.wykopmobilny.api.requests.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// POST /v3/links/draft/{key} - publikacja draftu jako znaleziska.
@JsonClass(generateAdapter = true)
data class LinkDraftPublishRequestV3(
    @field:Json(name = "title") val title: String,
    @field:Json(name = "description") val description: String,
    @field:Json(name = "tags") val tags: List<String>,
    @field:Json(name = "adult") val adult: Boolean,
    @field:Json(name = "photo") val photo: String? = null,
)

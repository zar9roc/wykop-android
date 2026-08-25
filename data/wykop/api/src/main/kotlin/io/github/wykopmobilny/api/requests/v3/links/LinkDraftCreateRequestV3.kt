package io.github.wykopmobilny.api.requests.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// POST /v3/links/draft - utworzenie draftu z URL.
@JsonClass(generateAdapter = true)
data class LinkDraftCreateRequestV3(
    @field:Json(name = "url") val url: String,
)

package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Odpowiedz POST /v3/links/draft. `similar` = potencjalne duplikaty (na razie nieuzywane).
@JsonClass(generateAdapter = true)
data class LinkDraftCreateResponseV3(
    @field:Json(name = "key") val key: String,
    @field:Json(name = "duplicate") val duplicate: Boolean? = null,
)

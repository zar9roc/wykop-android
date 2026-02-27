package io.github.wykopmobilny.api.responses.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CardResponseV3(
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "image") val image: String?,
    @field:Json(name = "source") val source: CardSourceResponseV3?,
)

@JsonClass(generateAdapter = true)
data class CardSourceResponseV3(
    @field:Json(name = "label") val label: String,
    @field:Json(name = "url") val url: String,
)

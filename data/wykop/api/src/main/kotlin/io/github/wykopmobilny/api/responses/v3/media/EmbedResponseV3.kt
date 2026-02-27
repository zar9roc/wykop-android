package io.github.wykopmobilny.api.responses.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EmbedResponseV3(
    @field:Json(name = "type") val type: String,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "key") val key: String?,
    @field:Json(name = "thumbnail") val thumbnail: String?,
)

package io.github.wykopmobilny.api.responses.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PhotoResponseV3(
    @field:Json(name = "url") val url: String,
    @field:Json(name = "key") val key: String?,
    @field:Json(name = "label") val label: String?,
    @field:Json(name = "mime_type") val mimeType: String?,
    @field:Json(name = "source") val source: String?,
    @field:Json(name = "plus18") val plus18: Boolean?,
    @field:Json(name = "width") val width: Int?,
    @field:Json(name = "height") val height: Int?,
)

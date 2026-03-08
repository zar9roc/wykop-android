package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SourceResponseV3(
    @field:Json(name = "label") val label: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "type") val type: String?,
    @field:Json(name = "type_id") val typeId: Int?,
)

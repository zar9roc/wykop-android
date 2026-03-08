package io.github.wykopmobilny.api.requests.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddRelatedRequestV3(
    @field:Json(name = "title") val title: String,
    @field:Json(name = "url") val url: String,
    @field:Json(name = "adult") val adult: Boolean? = null,
)

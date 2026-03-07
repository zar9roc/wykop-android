package io.github.wykopmobilny.api.requests.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateUpdateEntryRequestV3(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "photo") val photo: String? = null,
    @field:Json(name = "embed") val embed: String? = null,
    @field:Json(name = "survey") val survey: String? = null,
    @field:Json(name = "adult") val adult: Boolean? = null,
)

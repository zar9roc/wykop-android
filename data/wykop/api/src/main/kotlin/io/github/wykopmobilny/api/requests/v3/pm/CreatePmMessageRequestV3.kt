package io.github.wykopmobilny.api.requests.v3.pm

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreatePmMessageRequestV3(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "photo") val photo: String? = null,
    @field:Json(name = "embed") val embed: String? = null,
)

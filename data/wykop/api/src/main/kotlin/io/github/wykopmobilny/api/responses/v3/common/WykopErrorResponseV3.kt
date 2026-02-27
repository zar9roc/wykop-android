package io.github.wykopmobilny.api.responses.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WykopErrorResponseV3(
    @field:Json(name = "code") val code: Int,
    @field:Json(name = "hash") val hash: String?,
    @field:Json(name = "error") val error: ErrorDetailsV3?,
)

@JsonClass(generateAdapter = true)
data class ErrorDetailsV3(
    @field:Json(name = "message") val message: String,
    @field:Json(name = "key") val key: String?,
)

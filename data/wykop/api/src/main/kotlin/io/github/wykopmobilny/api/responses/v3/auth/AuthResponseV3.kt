package io.github.wykopmobilny.api.responses.v3.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResponseV3(
    @field:Json(name = "token") val token: String,
)

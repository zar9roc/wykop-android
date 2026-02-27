package io.github.wykopmobilny.api.requests.v3.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequestV3(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "password") val password: String,
)

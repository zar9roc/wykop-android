package io.github.wykopmobilny.api.requests.v3.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshTokenRequestV3(
    @field:Json(name = "refresh_token") val refreshToken: String,
)

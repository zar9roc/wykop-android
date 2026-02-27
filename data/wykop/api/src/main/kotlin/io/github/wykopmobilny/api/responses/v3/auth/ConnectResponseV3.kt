package io.github.wykopmobilny.api.responses.v3.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConnectResponseV3(
    @field:Json(name = "connect_url") val connectUrl: String,
)

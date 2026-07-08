package io.github.wykopmobilny.api.responses.v3.blacklist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlacklistedUserResponseV3(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "gender") val gender: String?,
    @field:Json(name = "color") val color: String?,
    @field:Json(name = "avatar") val avatar: String?,
)

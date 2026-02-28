package io.github.wykopmobilny.api.responses.v3.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserMeResponseV3(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "avatar") val avatar: String,
    @field:Json(name = "background") val background: String?,
    @field:Json(name = "color") val color: String?,
    @field:Json(name = "gender") val gender: String?,
    @field:Json(name = "verified") val verified: Boolean?,
    @field:Json(name = "sponsor") val sponsor: Boolean?,
    @field:Json(name = "online") val online: Boolean?,
)

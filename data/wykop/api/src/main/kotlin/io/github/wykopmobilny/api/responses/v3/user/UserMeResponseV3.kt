package io.github.wykopmobilny.api.responses.v3.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserMeResponseV3(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "company") val company: Boolean?,
    @field:Json(name = "gender") val gender: String?,
    @field:Json(name = "avatar") val avatar: String,
    @field:Json(name = "note") val note: Boolean?,
    @field:Json(name = "online") val online: Boolean?,
    @field:Json(name = "status") val status: String?,
    @field:Json(name = "color") val color: String?,
    @field:Json(name = "verified") val verified: Boolean?,
    @field:Json(name = "follow") val follow: Boolean?,
    @field:Json(name = "rank") val rank: UserRankResponseV3?,
    @field:Json(name = "donation") val donation: UserDonationResponseV3?,
    @field:Json(name = "actions") val actions: UserActionsResponseV3?,
)

@JsonClass(generateAdapter = true)
data class UserRankResponseV3(
    @field:Json(name = "position") val position: Int?,
    @field:Json(name = "trend") val trend: Int?,
)

@JsonClass(generateAdapter = true)
data class UserDonationResponseV3(
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "links") val links: Boolean?,
    @field:Json(name = "entries") val entries: Boolean?,
)

@JsonClass(generateAdapter = true)
data class UserActionsResponseV3(
    @field:Json(name = "update") val update: Boolean?,
    @field:Json(name = "update_gender") val updateGender: Boolean?,
    @field:Json(name = "update_note") val updateNote: Boolean?,
    @field:Json(name = "blacklist") val blacklist: Boolean?,
    @field:Json(name = "follow") val follow: Boolean?,
)

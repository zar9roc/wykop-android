package io.github.wykopmobilny.api.responses.v3.profile

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3

@JsonClass(generateAdapter = true)
data class BadgeResponseV3(
    @field:Json(name = "label") val label: String?,
    @field:Json(name = "slug") val slug: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "type") val type: String?,
    @field:Json(name = "media") val media: BadgeMediaResponseV3?,
    @field:Json(name = "color") val color: BadgeColorResponseV3?,
    @field:Json(name = "level") val level: Int?,
    @field:Json(name = "progress") val progress: Int?,
    @field:Json(name = "achieved_at") val achievedAt: String?,
)

@JsonClass(generateAdapter = true)
data class BadgeMediaResponseV3(
    @field:Json(name = "icon") val icon: PhotoResponseV3?,
)

@JsonClass(generateAdapter = true)
data class BadgeColorResponseV3(
    @field:Json(name = "hex") val hex: String?,
    @field:Json(name = "hex_dark") val hexDark: String?,
)

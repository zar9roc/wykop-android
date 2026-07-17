package io.github.wykopmobilny.api.responses.v3.config

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConfigResponseV3(
    @field:Json(name = "colors") val colors: List<ConfigColorV3>?,
)

// Kolor rangi nicka: hex dla trybu jasnego + hex_dark dla nocnego.
@JsonClass(generateAdapter = true)
data class ConfigColorV3(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "hex") val hex: String?,
    @field:Json(name = "hex_dark") val hexDark: String?,
)

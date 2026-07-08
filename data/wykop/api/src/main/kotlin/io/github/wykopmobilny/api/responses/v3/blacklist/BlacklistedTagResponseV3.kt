package io.github.wykopmobilny.api.responses.v3.blacklist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlacklistedTagResponseV3(
    @field:Json(name = "name") val name: String,
)

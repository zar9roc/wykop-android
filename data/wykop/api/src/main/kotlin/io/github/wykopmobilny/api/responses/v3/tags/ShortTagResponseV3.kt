package io.github.wykopmobilny.api.responses.v3.tags

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShortTagResponseV3(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "pinned") val pinned: Boolean?,
)

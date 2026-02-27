package io.github.wykopmobilny.api.responses.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VotesResponseV3(
    @field:Json(name = "up") val up: Int,
    @field:Json(name = "down") val down: Int,
)

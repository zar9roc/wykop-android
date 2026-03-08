package io.github.wykopmobilny.api.requests.v3.favourites

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FavouriteRequestV3(
    @field:Json(name = "type") val type: String,
    @field:Json(name = "source_id") val sourceId: Long,
)

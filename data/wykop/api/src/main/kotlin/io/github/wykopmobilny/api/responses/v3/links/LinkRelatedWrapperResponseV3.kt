package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LinkRelatedWrapperResponseV3(
    @field:Json(name = "items") val items: List<RelatedResponseV3>?,
)

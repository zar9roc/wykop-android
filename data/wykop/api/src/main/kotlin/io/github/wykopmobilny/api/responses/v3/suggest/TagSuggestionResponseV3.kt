package io.github.wykopmobilny.api.responses.v3.suggest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagSuggestionResponseV3(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "observed_qty") val observedQty: Int,
)

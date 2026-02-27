package io.github.wykopmobilny.api.responses.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaginationResponseV3(
    @field:Json(name = "per_page") val perPage: Int,
    @field:Json(name = "total") val total: Int,
    @field:Json(name = "next") val next: String?,
    @field:Json(name = "prev") val prev: String?,
)

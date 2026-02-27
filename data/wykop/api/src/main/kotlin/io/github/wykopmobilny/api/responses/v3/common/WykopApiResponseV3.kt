package io.github.wykopmobilny.api.responses.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WykopApiResponseV3<out T>(
    @field:Json(name = "data") val data: T?,
    @field:Json(name = "pagination") val pagination: PaginationResponseV3?,
)

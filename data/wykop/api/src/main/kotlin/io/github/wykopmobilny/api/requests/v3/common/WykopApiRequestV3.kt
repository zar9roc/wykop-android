package io.github.wykopmobilny.api.requests.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WykopApiRequestV3<T>(
    @field:Json(name = "data") val data: T,
)

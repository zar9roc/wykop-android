package io.github.wykopmobilny.api.requests.v3.blacklist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlacklistUserRequestV3(
    @field:Json(name = "username") val username: String,
)

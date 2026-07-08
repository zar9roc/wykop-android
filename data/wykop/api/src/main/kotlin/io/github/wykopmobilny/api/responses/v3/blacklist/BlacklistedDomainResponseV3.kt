package io.github.wykopmobilny.api.responses.v3.blacklist

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlacklistedDomainResponseV3(
    @field:Json(name = "domain") val domain: String,
)

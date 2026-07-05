package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3

/**
 * Element listy glosujacych GET /links/{id}/upvotes/{type}.
 * Realna odpowiedz API to obiekt {created_at, reason, user} - NIE goly UserShort
 * jak twierdzi spec OpenAPI. `reason` to etykieta ("duplicate", "spam", ...) dla
 * zakopujacych, pusty string dla wykopujacych.
 */
@JsonClass(generateAdapter = true)
data class LinkVoterResponseV3(
    @field:Json(name = "created_at") val createdAt: String?,
    @field:Json(name = "reason") val reason: String?,
    @field:Json(name = "user") val user: UserShortResponseV3,
)

package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import kotlinx.datetime.Instant

@JsonClass(generateAdapter = true)
data class RelatedResponseV3(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "author") val author: UserShortResponseV3,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "created_at") val createdAt: Instant,
    @field:Json(name = "voted") val voted: Int?,
    @field:Json(name = "votes") val votes: LinkVotesResponseV3,
    @field:Json(name = "adult") val adult: Boolean?,
)

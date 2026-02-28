package io.github.wykopmobilny.api.responses.v3.links

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.common.ActionsResponseV3
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import kotlinx.datetime.Instant

@JsonClass(generateAdapter = true)
data class LinkCommentResponseV3(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "author") val author: UserShortResponseV3,
    @field:Json(name = "device") val device: String?,
    @field:Json(name = "created_at") val createdAt: Instant,
    @field:Json(name = "voted") val voted: Int?,
    @field:Json(name = "content") val content: String?,
    @field:Json(name = "media") val media: MediaResponseV3?,
    @field:Json(name = "adult") val adult: Boolean?,
    @field:Json(name = "deletable") val deletable: Boolean?,
    @field:Json(name = "votes") val votes: LinkVotesResponseV3,
    @field:Json(name = "parent_id") val parentId: Long?,
    @field:Json(name = "resource") val resource: String,
    @field:Json(name = "actions") val actions: ActionsResponseV3?,
    @field:Json(name = "deleted") val deleted: Boolean?,
)

package io.github.wykopmobilny.api.responses.v3.notifications

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3

@JsonClass(generateAdapter = true)
data class NotificationEntryResponseV3(
    @field:Json(name = "type") override val type: String,
    @field:Json(name = "id") override val id: String,
    @field:Json(name = "read") override val read: Int?,
    @field:Json(name = "group_id") override val groupId: String?,
    @field:Json(name = "group_count") override val groupCount: Int?,
    @field:Json(name = "show_as_group") override val showAsGroup: Boolean?,
    @field:Json(name = "created_at") override val createdAt: String,
    @field:Json(name = "user") override val user: UserShortResponseV3?,
    @field:Json(name = "comment") val comment: NotificationCommentResponseV3?,
    @field:Json(name = "link") val link: LinkResponseV3?,
    @field:Json(name = "entry") val entry: EntryResponseV3?,
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "badge") val badge: NotificationBadgeResponseV3?,
) : NotificationResponseV3

@JsonClass(generateAdapter = true)
data class NotificationCommentResponseV3(
    @field:Json(name = "id") val id: Int?,
)

@JsonClass(generateAdapter = true)
data class NotificationBadgeResponseV3(
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "slug") val slug: String?,
)

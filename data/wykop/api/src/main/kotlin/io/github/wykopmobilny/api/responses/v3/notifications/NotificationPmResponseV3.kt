package io.github.wykopmobilny.api.responses.v3.notifications

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3

@JsonClass(generateAdapter = true)
data class NotificationPmResponseV3(
    @field:Json(name = "type") override val type: String,
    @field:Json(name = "id") override val id: String,
    @field:Json(name = "read") override val read: Int?,
    @field:Json(name = "group_id") override val groupId: String?,
    @field:Json(name = "group_count") override val groupCount: Int?,
    @field:Json(name = "show_as_group") override val showAsGroup: Boolean?,
    @field:Json(name = "created_at") override val createdAt: String,
    @field:Json(name = "user") override val user: UserShortResponseV3?,
    @field:Json(name = "content") val content: String?,
) : NotificationResponseV3

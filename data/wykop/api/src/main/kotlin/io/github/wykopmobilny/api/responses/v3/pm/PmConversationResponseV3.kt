package io.github.wykopmobilny.api.responses.v3.pm

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3

@JsonClass(generateAdapter = true)
data class PmConversationResponseV3(
    @field:Json(name = "user") val user: UserShortResponseV3,
    @field:Json(name = "last_message") val lastMessage: PmMessageResponseV3?,
    @field:Json(name = "unread") val unread: Boolean?,
)

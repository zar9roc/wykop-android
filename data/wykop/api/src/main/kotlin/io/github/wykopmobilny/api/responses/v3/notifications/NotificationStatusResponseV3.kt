package io.github.wykopmobilny.api.responses.v3.notifications

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationStatusResponseV3(
    @field:Json(name = "pm") val pm: Boolean?,
    @field:Json(name = "pm_notification") val pmNotification: Boolean?,
    @field:Json(name = "entry_notification") val entryNotification: Boolean?,
    @field:Json(name = "tag_notification") val tagNotification: Boolean?,
    @field:Json(name = "observed_discussions_notification") val observedDiscussionsNotification: Boolean?,
    @field:Json(name = "pm_notification_count") val pmNotificationCount: Int?,
    @field:Json(name = "entry_notification_count") val entryNotificationCount: Int?,
    @field:Json(name = "tag_notification_count") val tagNotificationCount: Int?,
    @field:Json(name = "observed_discussions_notification_count") val observedDiscussionsNotificationCount: Int?,
)

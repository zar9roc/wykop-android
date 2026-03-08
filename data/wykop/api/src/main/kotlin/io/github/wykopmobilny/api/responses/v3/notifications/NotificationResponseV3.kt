package io.github.wykopmobilny.api.responses.v3.notifications

import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3

/**
 * Sealed interface representing all notification types in API v3.
 * Common fields shared by all notification types.
 */
sealed interface NotificationResponseV3 {
    val id: String
    val type: String
    val read: Int?
    val groupId: String?
    val groupCount: Int?
    val showAsGroup: Boolean?
    val createdAt: String
    val user: UserShortResponseV3?
}

/**
 * Extension property to get notification body/message content.
 * Implementation varies by notification type.
 */
val NotificationResponseV3.body: String
    get() =
        when (this) {
            is NotificationEntryResponseV3 -> message.orEmpty()
            is NotificationTagResponseV3 -> tag?.name.orEmpty()
            is NotificationPmResponseV3 -> content.orEmpty()
            else -> ""
        }

/**
 * Extension property to get notification URL if available.
 * Implementation varies by notification type.
 */
val NotificationResponseV3.notificationUrl: String?
    get() =
        when (this) {
            is NotificationEntryResponseV3 -> url
            else -> null
        }

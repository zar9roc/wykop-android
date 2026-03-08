package io.github.wykopmobilny.api.notifications

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.NotificationsV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.responses.NotificationsCountResponse
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationEntryResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationStatusResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationTagResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.body
import io.github.wykopmobilny.api.responses.v3.notifications.notificationUrl
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
import kotlinx.coroutines.rx2.rxSingle
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Extension function to convert NotificationResponseV3 to legacy Notification model.
 * Eliminates the need for separate mapper classes.
 */
private fun NotificationResponseV3.toNotification() =
    Notification(
        id = id.toLongOrNull() ?: 0L,
        author = user?.let(AuthorMapperV3::map),
        body = body,
        date = runCatching { Instant.parse(createdAt) }.getOrNull(),
        type = type,
        url = notificationUrl,
        new = (read ?: 0) == 0,
    )

class NotificationsRepository
    @Inject
    constructor(
        private val notificationsApiV3: NotificationsV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : NotificationsApi {
        override fun readNotifications() =
            rxSingle { notificationsApiV3.markAllEntryNotificationsAsRead() }
                .retryWhen(userTokenRefresher)
                .map { emptyList<Notification>() }

        override fun getNotifications(page: Int) =
            rxSingle { notificationsApiV3.getEntryNotifications(page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<NotificationEntryResponseV3>>(errorBodyParser))
                .map { it.map { response -> response.toNotification() } }

        override fun getNotificationCount() =
            rxSingle { notificationsApiV3.getNotificationStatus() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<NotificationStatusResponseV3>(errorBodyParser))
                .map {
                    NotificationsCountResponse(
                        count = it.entryNotificationCount ?: 0,
//                        hashtags = it.tagNotificationCount ?: 0,
                    )
                }

        override fun readHashTagNotifications() =
            rxSingle { notificationsApiV3.markAllTagNotificationsAsRead() }
                .retryWhen(userTokenRefresher)
                .map { emptyList<Notification>() }

        override fun getHashTagNotifications(page: Int) =
            rxSingle { notificationsApiV3.getTagNotifications(page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<NotificationTagResponseV3>>(errorBodyParser))
                .map { it.map { response -> response.toNotification() } }

        override fun getHashTagNotificationCount() =
            rxSingle { notificationsApiV3.getNotificationStatus() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<NotificationStatusResponseV3>(errorBodyParser))
                .map {
                    NotificationsCountResponse(
//                        entries = it.entryNotificationCount ?: 0,
                        count = it.tagNotificationCount ?: 0,
                    )
                }
    }

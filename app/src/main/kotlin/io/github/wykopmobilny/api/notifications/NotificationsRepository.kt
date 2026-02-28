package io.github.wykopmobilny.api.notifications

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.NotificationsV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.responses.NotificationsCountResponse
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationEntryResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationStatusResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationTagResponseV3
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.mapper.apiv3.NotificationEntryMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.NotificationTagMapperV3
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class NotificationsRepository
    @Inject
    constructor(
        private val notificationsApiV3: NotificationsV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
    ) : NotificationsApi {
        override fun readNotifications() =
            rxSingle { notificationsApiV3.markAllEntryNotificationsAsRead() }
                .retryWhen(userTokenRefresher)
                .map { emptyList<Notification>() }

        override fun getNotifications(page: Int) =
            rxSingle { notificationsApiV3.getEntryNotifications(page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<NotificationEntryResponseV3>>())
                .map { it.map { response -> NotificationEntryMapperV3.map(response) } }

        override fun getNotificationCount() =
            rxSingle { notificationsApiV3.getNotificationStatus() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<NotificationStatusResponseV3>())
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
                .compose(ErrorHandlerTransformerV3<List<NotificationTagResponseV3>>())
                .map { it.map { response -> NotificationTagMapperV3.map(response) } }

        override fun getHashTagNotificationCount() =
            rxSingle { notificationsApiV3.getNotificationStatus() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<NotificationStatusResponseV3>())
                .map {
                    NotificationsCountResponse(
//                        entries = it.entryNotificationCount ?: 0,
                        count = it.tagNotificationCount ?: 0,
                    )
                }
    }

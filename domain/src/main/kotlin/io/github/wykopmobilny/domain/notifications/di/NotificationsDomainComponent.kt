package io.github.wykopmobilny.domain.notifications.di

import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import io.github.wykopmobilny.api.endpoints.v3.NotificationsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.PmV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.pm.CreatePmMessageRequestV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.data.storage.api.ReadNotificationEntity
import io.github.wykopmobilny.notification.AppNotification
import io.github.wykopmobilny.notification.HandleNotificationDismissed
import io.github.wykopmobilny.notification.MarkChannelRead
import io.github.wykopmobilny.notification.MarkNotificationRead
import io.github.wykopmobilny.notification.FrequentPollingInterval
import io.github.wykopmobilny.notification.NotificationDependencies
import io.github.wykopmobilny.notification.RefreshNotifications
import io.github.wykopmobilny.notification.SendPrivateMessageReply
import io.github.wykopmobilny.domain.settings.prefs.GetNotificationPreferences
import io.github.wykopmobilny.domain.work.RefreshNotificationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject

@Subcomponent(modules = [NotificationsModule::class])
interface NotificationsDomainComponent : NotificationDependencies

@Module
internal abstract class NotificationsModule {
    @Binds
    abstract fun handleNotificationDismissed(impl: HandleNotificationDismissedImpl): HandleNotificationDismissed

    @Binds
    abstract fun markNotificationRead(impl: MarkNotificationReadImpl): MarkNotificationRead

    @Binds
    abstract fun markChannelRead(impl: MarkChannelReadImpl): MarkChannelRead

    @Binds
    abstract fun sendPrivateMessageReply(impl: SendPrivateMessageReplyImpl): SendPrivateMessageReply

    @Binds
    abstract fun refreshNotifications(impl: RefreshNotificationsUseCase): RefreshNotifications

    @Binds
    abstract fun frequentPollingInterval(impl: FrequentPollingIntervalImpl): FrequentPollingInterval
}

// Interwal foreground pollingu: okres < 15 min wybrany w "Czestotliwosci sprawdzania"
// (WorkManager nie schodzi nizej); null = tryb wylaczony.
internal class FrequentPollingIntervalImpl
    @Inject
    constructor(
        private val getNotificationPreferences: GetNotificationPreferences,
    ) : FrequentPollingInterval {
        override fun invoke(): Flow<Duration?> =
            getNotificationPreferences()
                .map { prefs ->
                    prefs.notificationRefreshPeriod.duration
                        .takeIf { prefs.notificationsEnabled && it < 15.minutes }
                }.distinctUntilChanged()
    }

// Zapis odrzuconych zdarzen po id (stableNotificationId) - dismissedAt = moment odrzucenia,
// wiec kazde zdarzenie starsze od tej chwili (czyli to konkretne) nie wroci przy kolejnym
// odswiezeniu. Wczesniejsza wersja czytala martwy store v1/v2 i oznaczala wszystko.
internal class HandleNotificationDismissedImpl
    @Inject
    constructor(
        private val appStorage: AppStorage,
    ) : HandleNotificationDismissed {
        override suspend fun invoke(dismissedIds: List<Long>) =
            withContext(AppDispatchers.IO) {
                val now = Clock.System.now()
                appStorage.notificationsQueries.transaction {
                    dismissedIds.forEach { id ->
                        appStorage.notificationsQueries.insertOrReplace(
                            ReadNotificationEntity(
                                notificationId = id,
                                dismissedAt = now,
                            ),
                        )
                    }
                }
            }
    }

// "Oznacz jako przeczytane" - PUT na endpoint wlasciwy dla typu powiadomienia.
internal class MarkNotificationReadImpl
    @Inject
    constructor(
        private val notificationsApi: NotificationsV3RetrofitApi,
    ) : MarkNotificationRead {
        override suspend fun invoke(
            channel: AppNotification.Channel,
            notificationId: String,
        ) {
            when (channel) {
                AppNotification.Channel.TO_ME -> notificationsApi.markEntryNotificationAsRead(notificationId)
                AppNotification.Channel.PRIVATE_MESSAGES -> notificationsApi.markPmNotificationAsRead(notificationId)
                AppNotification.Channel.TAGS -> notificationsApi.markTagNotificationAsRead(notificationId)
                AppNotification.Channel.OBSERVED_DISCUSSIONS ->
                    notificationsApi.markObservedDiscussionNotificationAsRead(notificationId)
            }
        }
    }

internal class MarkChannelReadImpl
    @Inject
    constructor(
        private val notificationsApi: NotificationsV3RetrofitApi,
    ) : MarkChannelRead {
        override suspend fun invoke(channel: AppNotification.Channel) {
            when (channel) {
                AppNotification.Channel.TO_ME -> notificationsApi.markAllEntryNotificationsAsRead()
                AppNotification.Channel.PRIVATE_MESSAGES -> notificationsApi.markAllPmNotificationsAsRead()
                AppNotification.Channel.TAGS -> notificationsApi.markAllTagNotificationsAsRead()
                AppNotification.Channel.OBSERVED_DISCUSSIONS ->
                    notificationsApi.markAllObservedDiscussionNotificationsAsRead()
            }
        }
    }

// Direct reply z powiadomienia PM.
internal class SendPrivateMessageReplyImpl
    @Inject
    constructor(
        private val pmApi: PmV3RetrofitApi,
    ) : SendPrivateMessageReply {
        override suspend fun invoke(
            username: String,
            content: String,
        ) {
            pmApi.sendMessage(
                username = username,
                body = WykopApiRequestV3(CreatePmMessageRequestV3(content = content)),
            )
        }
    }

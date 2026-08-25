package io.github.wykopmobilny.domain.work

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.NotificationsV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationEntryResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationObservedDiscussionResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationPmResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationTagResponseV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.notification.AppNotification
import io.github.wykopmobilny.notification.NotificationsManager
import io.github.wykopmobilny.notification.RefreshNotifications
import io.github.wykopmobilny.notification.stableId
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject

private val apiTimeZone = TimeZone.of("Europe/Warsaw")
private const val EXCERPT_LENGTH = 120

/**
 * Wspolna logika odswiezania powiadomien (worker 15-min i foreground polling co ~1 min).
 *
 * Tryb statusFirst: jeden tani GET /notifications/status; listy kanalow pobierane
 * tylko gdy licznik > 0, licznik 0 publikuje pusty stan (zdejmuje przeczytane gdzie
 * indziej). Obejmuje wszystkie 4 kanaly - spojna swiezosc niezaleznie od typu.
 */
internal class RefreshNotificationsUseCase
    @Inject
    constructor(
        private val jwtTokenStorage: JwtTokenStorage,
        private val notificationsApi: NotificationsV3RetrofitApi,
        private val notificationsManager: NotificationsManager,
        private val appStorage: AppStorage,
    ) : RefreshNotifications {
        override suspend fun invoke(statusFirst: Boolean) {
            if (jwtTokenStorage.jwtToken.first() == null) {
                Napier.i("User not logged in, skipping notification refresh")
                return
            }
            if (statusFirst) {
                refreshStatusFirst()
            } else {
                refreshAll()
            }
        }

        private suspend fun refreshStatusFirst() {
            val status = notificationsApi.getNotificationStatus().data ?: return
            coroutineScope {
                val toMe =
                    async {
                        if ((status.entryNotificationCount ?: 0) > 0) {
                            notificationsApi.getEntryNotifications(page = 1).data.orEmpty().map { it.toAppNotification() }
                        } else {
                            emptyList()
                        }
                    }
                val pm =
                    async {
                        if ((status.pmNotificationCount ?: 0) > 0) {
                            notificationsApi.getPmNotifications(page = 1).data.orEmpty().map { it.toAppNotification() }
                        } else {
                            emptyList()
                        }
                    }
                val tags =
                    async {
                        if ((status.tagNotificationCount ?: 0) > 0) {
                            notificationsApi.getTagNotifications(page = 1).data.orEmpty().map { it.toAppNotification() }
                        } else {
                            emptyList()
                        }
                    }
                val discussions =
                    async {
                        if ((status.observedDiscussionsNotificationCount ?: 0) > 0) {
                            notificationsApi
                                .getObservedDiscussionNotifications(page = 1)
                                .data
                                .orEmpty()
                                .map { it.toAppNotification() }
                        } else {
                            emptyList()
                        }
                    }
                publishChannel(AppNotification.Channel.TO_ME, toMe.await())
                publishChannel(AppNotification.Channel.PRIVATE_MESSAGES, pm.await())
                publishChannel(AppNotification.Channel.TAGS, tags.await())
                publishChannel(AppNotification.Channel.OBSERVED_DISCUSSIONS, discussions.await())
            }
        }

        private suspend fun refreshAll() {
            coroutineScope {
                val toMe =
                    async {
                        notificationsApi.getEntryNotifications(page = 1).data.orEmpty().map { it.toAppNotification() }
                    }
                val pm =
                    async {
                        notificationsApi.getPmNotifications(page = 1).data.orEmpty().map { it.toAppNotification() }
                    }
                val tags =
                    async {
                        notificationsApi.getTagNotifications(page = 1).data.orEmpty().map { it.toAppNotification() }
                    }
                val discussions =
                    async {
                        notificationsApi
                            .getObservedDiscussionNotifications(page = 1)
                            .data
                            .orEmpty()
                            .map { it.toAppNotification() }
                    }

                publishChannel(AppNotification.Channel.TO_ME, toMe.await())
                publishChannel(AppNotification.Channel.PRIVATE_MESSAGES, pm.await())
                publishChannel(AppNotification.Channel.TAGS, tags.await())
                publishChannel(AppNotification.Channel.OBSERVED_DISCUSSIONS, discussions.await())
            }
        }

        private suspend fun publishChannel(
            channel: AppNotification.Channel,
            notifications: List<Pair<AppNotification, Boolean>>,
        ) {
            val unread = notifications.filter { (_, isNew) -> isNew }.map { (notification, _) -> notification }
            val fresh =
                unread.filter { notification ->
                    withContext(AppDispatchers.IO) {
                        appStorage.notificationsQueries
                            .getById(notification.stableId())
                            .executeAsOneOrNull() == null
                    }
                }
            Napier.i("Channel $channel: ${fresh.size}(${unread.size}) out of ${notifications.size}")
            notificationsManager.publish(channel, fresh)
        }
    }

// ==================== Mapowanie v3 -> AppNotification ====================
// (isNew osobno - filtracja przed publikacja)

internal fun NotificationEntryResponseV3.toAppNotification(): Pair<AppNotification, Boolean> {
    val action =
        when (type) {
            "new_comment_in_entry" -> "skomentował(a) wpis"
            "new_entry" -> "dodał(a) wpis"
            "new_comment_in_link" -> "skomentował(a) znalezisko"
            "new_link" -> "dodał(a) znalezisko"
            "new_follower" -> "obserwuje Cię"
            else -> null
        }
    val username = user?.username
    val excerpt = (entry?.content ?: link?.title)?.stripHtml()?.take(EXCERPT_LENGTH)
    val title =
        when {
            username != null && action != null -> "$username $action"
            badge?.name != null -> "Nowa odznaka"
            else -> "Powiadomienie"
        }
    val body =
        when {
            username != null && action != null && !excerpt.isNullOrBlank() && type != "new_follower" -> excerpt
            !message.isNullOrBlank() -> message!!
            badge?.name != null -> "Otrzymano odznakę: ${badge?.name}"
            else -> title
        }
    val commentAnchor = comment?.id?.let { "/#comment-$it" }.orEmpty()
    val interopUrl =
        when {
            entry != null -> "https://wykop.pl/wpis/${entry?.id}$commentAnchor"
            link != null -> "https://wykop.pl/link/${link?.id}$commentAnchor"
            type == "new_follower" && username != null -> "https://wykop.pl/ludzie/$username"
            else -> url
        }
    return AppNotification(
        id = id,
        title = title,
        message = body,
        timestampMs = createdAt.toMillisOrNull(),
        channel = AppNotification.Channel.TO_ME,
        interopUrl = interopUrl,
        avatarUrl = user?.avatar,
    ) to ((read ?: 0) == 0)
}

internal fun NotificationPmResponseV3.toAppNotification(): Pair<AppNotification, Boolean> {
    val username = user?.username
    return AppNotification(
        id = id,
        title = username?.let { "Wiadomość od $it" } ?: "Nowa wiadomość",
        message = content?.stripHtml()?.take(EXCERPT_LENGTH).orEmpty().ifBlank { "Nowa wiadomość prywatna" },
        timestampMs = createdAt.toMillisOrNull(),
        channel = AppNotification.Channel.PRIVATE_MESSAGES,
        interopUrl = username?.let { "https://wykop.pl/wiadomosc-prywatna/konwersacja/$it" },
        conversationUser = username,
        avatarUrl = user?.avatar,
    ) to ((read ?: 0) == 0)
}

internal fun NotificationTagResponseV3.toAppNotification(): Pair<AppNotification, Boolean> {
    val tagName = tag?.name
    val action =
        when (type) {
            "new_link_with_observed_tag" -> "dodał(a) znalezisko z tagiem"
            else -> "dodał(a) wpis z tagiem"
        }
    val title = listOfNotNull(user?.username, action, tagName?.let { "#$it" }).joinToString(" ")
    val excerpt = (entry?.content ?: link?.title)?.stripHtml()?.take(EXCERPT_LENGTH)
    val interopUrl =
        when {
            entry != null -> "https://wykop.pl/wpis/${entry?.id}"
            link != null -> "https://wykop.pl/link/${link?.id}"
            tagName != null -> "https://wykop.pl/tag/$tagName"
            else -> null
        }
    return AppNotification(
        id = id,
        title = title.ifBlank { "Obserwowany tag" },
        message = excerpt.orEmpty().ifBlank { title },
        timestampMs = createdAt.toMillisOrNull(),
        channel = AppNotification.Channel.TAGS,
        interopUrl = interopUrl,
        avatarUrl = user?.avatar,
    ) to ((read ?: 0) == 0)
}

internal fun NotificationObservedDiscussionResponseV3.toAppNotification(): Pair<AppNotification, Boolean> {
    val excerpt = (entry?.content ?: link?.title)?.stripHtml()?.take(EXCERPT_LENGTH)
    val interopUrl =
        when {
            entry != null -> "https://wykop.pl/wpis/${entry?.id}"
            link != null -> "https://wykop.pl/link/${link?.id}"
            else -> null
        }
    return AppNotification(
        id = id,
        title = "Nowa aktywność w obserwowanej dyskusji",
        message = excerpt.orEmpty().ifBlank { "Nowe komentarze w obserwowanej dyskusji" },
        timestampMs = createdAt.toMillisOrNull(),
        channel = AppNotification.Channel.OBSERVED_DISCUSSIONS,
        interopUrl = interopUrl,
        avatarUrl = user?.avatar,
    ) to ((read ?: 0) == 0)
}

private fun String.toMillisOrNull(): Long? =
    runCatching { LocalDateTime.parse(replace(' ', 'T')).toInstant(apiTimeZone) }
        .getOrNull()
        ?.toEpochMilliseconds()

private fun String.stripHtml(): String = replace(Regex("<[^>]*>"), "").trim()

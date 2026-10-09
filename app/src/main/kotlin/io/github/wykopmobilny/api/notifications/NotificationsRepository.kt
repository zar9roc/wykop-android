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
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationTargetKind
import io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
import io.github.wykopmobilny.utils.textview.removeHtml
import kotlinx.coroutines.rx2.rxSingle
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject

private val apiTimeZone = TimeZone.of("Europe/Warsaw")

/**
 * Extension function to convert NotificationResponseV3 to legacy Notification model.
 * Eliminates the need for separate mapper classes.
 */
private fun NotificationResponseV3.toNotification() =
    Notification(
        // Identyfikator z API v3 jest tekstowy (hash) - trzymamy go bez konwersji;
        // probowanie toLong() dawalo 0 dla kazdego powiadomienia.
        id = id,
        author = user?.let(AuthorMapperV3::map),
        body = buildBody(),
        date = runCatching { LocalDateTime.parse(createdAt.replace(' ', 'T')).toInstant(apiTimeZone) }.getOrNull(),
        type = type,
        url = buildUrl(),
        new = (read ?: 0) == 0,
        targetKind = targetKind(),
        targetSlug = targetSlug(),
        targetTitle = targetTitle(),
    )

/**
 * Rodzaj celu powiadomienia. API v3 nie mowi wprost, czy chodzi o wpis czy znalezisko -
 * rozroznia to dolaczony obiekt `entry` / `link`.
 */
private fun NotificationResponseV3.targetKind(): NotificationTargetKind =
    when (this) {
        is NotificationEntryResponseV3 ->
            when {
                entry != null -> NotificationTargetKind.ENTRY
                link != null -> NotificationTargetKind.LINK
                else -> NotificationTargetKind.OTHER
            }

        else -> NotificationTargetKind.OTHER
    }

/** Slug celu - fallback tresci wiersza zbiorczego zakladki "Do mnie". */
private fun NotificationResponseV3.targetSlug(): String? =
    when (this) {
        is NotificationEntryResponseV3 -> (entry?.slug ?: link?.slug)?.takeIf { it.isNotBlank() }
        else -> null
    }

/**
 * Tresc celu - wlasciwa tresc wiersza zbiorczego zakladki "Do mnie" (slug jest
 * tylko fallbackiem). Dla wpisu bierzemy jego tresc, dla znaleziska tytul.
 */
private fun NotificationResponseV3.targetTitle(): String? =
    when (this) {
        is NotificationEntryResponseV3 -> excerptOf(entry?.content ?: link?.title)
        else -> null
    }

/**
 * Oczyszczenie i przyciecie tresci: bez HTML, bez lamania wierszy i wielokrotnych
 * spacji, z wielokropkiem gdy tekst zostal urwany.
 */
private fun excerptOf(raw: String?): String? {
    val clean =
        raw
            ?.removeHtml()
            ?.replace(whitespaceRegex, " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
    return if (clean.length <= EXCERPT_LENGTH) clean else clean.take(EXCERPT_LENGTH).trimEnd() + "\u2026"
}

private val whitespaceRegex = "\\s+".toRegex()

/**
 * API v3 wypelnia pole `url` tylko dla powiadomien systemowych - dla pozostalych
 * URL trzeba zbudowac z obiektow entry/link/comment w formacie rozumianym
 * przez WykopLinkHandler (EntryLinkParser/LinkParser).
 */
private fun NotificationResponseV3.buildUrl(): String? =
    when (this) {
        is NotificationEntryResponseV3 -> {
            val entry = entry
            val link = link
            val username = user?.username
            // URL budowany z obiektow ma pierwszenstwo przed polem `url` z API -
            // gwarantuje format kotwicy "/#comment-" rozumiany przez LinkParser/EntryLinkParser
            // (nawigacja + scroll do komentarza).
            when {
                entry != null -> entryUrl(entry.id, comment?.id)
                link != null -> linkUrl(link.id, comment?.id)
                type == "new_follower" && username != null -> "https://wykop.pl/ludzie/$username"
                else -> url.toAbsoluteWykopUrl()
            }
        }

        is NotificationTagResponseV3 -> {
            val entry = entry
            val link = link
            val tagName = tag?.name
            when {
                entry != null -> entryUrl(entry.id, null)
                link != null -> linkUrl(link.id, null)
                tagName != null -> "https://wykop.pl/tag/$tagName"
                else -> null
            }
        }

        else -> null
    }

/**
 * Pole `url` z API bywa adresem WZGLEDNYM - powiadomienie systemowe "Sprawdz nowosci
 * w changelogu!" przysyla "/changelog". Taki tekst po Uri.parse nie ma schematu, wiec
 * ACTION_VIEW nie ma czym go otworzyc. Doklejamy domene Wykopu, a pusty tekst
 * traktujemy jak brak celu.
 */
private fun String?.toAbsoluteWykopUrl(): String? {
    val raw = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return when {
        raw.contains("://") -> raw
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("/") -> WYKOP_BASE_URL + raw
        else -> "$WYKOP_BASE_URL/$raw"
    }
}

private const val WYKOP_BASE_URL = "https://wykop.pl"

private fun entryUrl(
    entryId: Long,
    commentId: Int?,
) = "https://wykop.pl/wpis/$entryId" + if (commentId != null) "/#comment-$commentId" else ""

private fun linkUrl(
    linkId: Long,
    commentId: Int?,
) = "https://wykop.pl/link/$linkId" + if (commentId != null) "/#comment-$commentId" else ""

/**
 * API v3 wypelnia `message` tylko dla powiadomien systemowych - tresc pozostalych
 * budujemy z typu i autora. Format "{nick} {akcja}" jest wymagany przez
 * NotificationViewHolder (koloruje nick = body.substringBefore(" ")), a dla tagow
 * "#nazwa" na koncu zasila Notification.tag (grupowanie w zakladce tagow).
 */
private fun NotificationResponseV3.buildBody(): String =
    when (this) {
        is NotificationEntryResponseV3 -> {
            val action =
                when (type) {
                    "new_comment_in_entry" -> "skomentował(a) wpis"
                    "new_entry" -> "dodał(a) wpis"
                    "new_comment_in_link" -> "skomentował(a) znalezisko"
                    "new_link" -> "dodał(a) znalezisko"
                    "new_follower" -> "obserwuje Cię"
                    // Generyczna odpowiedz na moja tresc - API nie mowi wprost czy chodzi
                    // o wpis czy znalezisko, wiec rozrozniamy po dolaczonym obiekcie.
                    "new_reply_to_my_content" ->
                        when {
                            entry != null -> "odpowiedział(a) Ci we wpisie"
                            link != null -> "odpowiedział(a) Ci w znalezisku"
                            else -> "odpowiedział(a) na Twoją treść"
                        }

                    else -> null
                }
            // Typy bez autora akcji (dotycza mojej tresci) - uzywane dopiero gdy API
            // nie przyslalo wlasnej tresci w `message`.
            val selfAction =
                when (type) {
                    "link_in_upcoming" -> "Twoje znalezisko trafiło do wykopalisk"
                    "link_on_homepage" -> "Twoje znalezisko trafiło na stronę główną"
                    "link_was_buried" -> "Twoje znalezisko zostało zakopane"
                    "moderation_action" -> "Akcja moderacyjna dotycząca Twojej treści"
                    "new_issue_response" -> "Odpowiedź na Twoje zgłoszenie"
                    else -> null
                }
            val username = user?.username
            val systemMessage = message
            val badgeName = badge?.name
            val excerpt = (entry?.content ?: link?.title)?.removeHtml()?.take(EXCERPT_LENGTH)
            when {
                username != null && action != null ->
                    "$username $action" + if (excerpt.isNullOrBlank() || type == "new_follower") "" else ": $excerpt"

                !systemMessage.isNullOrBlank() -> systemMessage

                badgeName != null -> "Otrzymano odznakę: $badgeName"

                selfAction != null -> selfAction

                // Nigdy nie pokazujemy surowej wartosci `type` - nieznany typ dostaje
                // neutralna tresc, a nick (jesli jest) zostaje na pierwszej pozycji.
                username != null -> "$username — nowa aktywność"

                else -> "Nowe powiadomienie"
            }
        }

        is NotificationTagResponseV3 -> {
            val action =
                when (type) {
                    "new_link_with_observed_tag" -> "dodał(a) znalezisko z tagiem"
                    else -> "dodał(a) wpis z tagiem"
                }
            listOfNotNull(user?.username, action, tag?.name?.let { "#$it" }).joinToString(" ")
        }

        else -> ""
    }

private const val EXCERPT_LENGTH = 120

class NotificationsRepository
    @Inject
    constructor(
        private val notificationsApiV3: NotificationsV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : NotificationsApi {
        override fun readNotifications() =
            rxSingle {
                notificationsApiV3.markAllEntryNotificationsAsRead()
                emptyList<Notification>()
            }.retryWhen(userTokenRefresher)

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
            rxSingle {
                notificationsApiV3.markAllTagNotificationsAsRead()
                emptyList<Notification>()
            }.retryWhen(userTokenRefresher)

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

        override fun getMessagesNotificationCount() =
            rxSingle { notificationsApiV3.getNotificationStatus() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<NotificationStatusResponseV3>(errorBodyParser))
                .map { NotificationsCountResponse(count = it.pmNotificationCount ?: 0) }
    }

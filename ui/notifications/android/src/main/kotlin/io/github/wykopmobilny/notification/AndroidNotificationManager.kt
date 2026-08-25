package io.github.wykopmobilny.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import dagger.Reusable
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.ui.notifications.android.R
import io.github.wykopmobilny.utils.requireDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Reusable
internal class AndroidNotificationManager
    @Inject
    constructor(
        private val context: Context,
        private val interopIntentHandler: @JvmSuppressWildcards (String?) -> Intent,
    ) : NotificationsManager {
        private val manager = context.getSystemService<NotificationManager>().let(::checkNotNull)

        override suspend fun publish(
            channel: AppNotification.Channel,
            notifications: List<AppNotification>,
        ) {
            ensureChannels()
            // Bez zgody na powiadomienia (Android 13+) notify() jest ignorowane - nie ma
            // sensu publikowac ani sledzic stanu.
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

            if (channel == AppNotification.Channel.PRIVATE_MESSAGES) {
                publishConversations(notifications)
            } else {
                publishEvents(channel, notifications)
            }
        }

        // ==================== Zwykle kanaly: zdarzenie = powiadomienie ====================

        private suspend fun publishEvents(
            channel: AppNotification.Channel,
            notifications: List<AppNotification>,
        ) {
            val groupKey = groupKey(channel)

            // Anuluj zdarzenia pokazane wczesniej, ktorych nie ma juz na liscie
            // (przeczytane w aplikacji/na stronie) - stan w shade = stan serwera.
            val currentTags = notifications.map { it.id }.toSet()
            cancelStale(groupKey, currentTags)

            if (notifications.isEmpty()) {
                manager.cancel(groupKey, SUMMARY_NOTIFICATION_ID)
                return
            }

            notifications.forEach { notification ->
                manager.notify(notification.id, EVENT_NOTIFICATION_ID, buildEventNotification(channel, notification))
            }
            manager.notify(groupKey, SUMMARY_NOTIFICATION_ID, buildSummaryNotification(channel, notifications))
        }

        private suspend fun buildEventNotification(
            channel: AppNotification.Channel,
            notification: AppNotification,
        ) = NotificationCompat
            .Builder(context, channelId(channel))
            .setSmallIcon(R.drawable.ic_wykopmobilny)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setCategory(channel.category)
            .setGroup(groupKey(channel))
            .setAutoCancel(true)
            .apply {
                notification.timestampMs?.let {
                    setWhen(it)
                    setShowWhen(true)
                }
                loadAvatar(notification.avatarUrl)?.let(::setLargeIcon)
            }.setDeleteIntent(dismissIntent(notification.id.hashCode(), longArrayOf(notification.stableId())))
            .setContentIntent(contentIntent(notification.id.hashCode(), notification.interopUrl))
            .addAction(
                markReadAction(
                    channel = channel,
                    requestCode = notification.id.hashCode(),
                    notificationIds = arrayOf(notification.id),
                    cancelTag = notification.id,
                    dismissedIds = longArrayOf(notification.stableId()),
                ),
            ).build()

        // ==================== PM: konwersacje (MessagingStyle + skroty) ====================

        // Wiadomosci grupowane per rozmowca: jedno powiadomienie-konwersacja (tag "pm:{user}")
        // z pelna historia nieprzeczytanych (MessagingStyle), osoba z awatarem i skrotem
        // long-lived -> na Androidzie 11+ laduje w sekcji "Konwersacje", mozna przypiac.
        private suspend fun publishConversations(notifications: List<AppNotification>) {
            val groupKey = groupKey(AppNotification.Channel.PRIVATE_MESSAGES)
            val conversations =
                notifications
                    .filter { it.conversationUser != null }
                    .groupBy { it.conversationUser.let(::checkNotNull) }

            val currentTags = conversations.keys.map { conversationTag(it) }.toSet()
            cancelStale(groupKey, currentTags)

            if (conversations.isEmpty()) {
                manager.cancel(groupKey, SUMMARY_NOTIFICATION_ID)
                return
            }

            conversations.forEach { (user, messages) ->
                manager.notify(
                    conversationTag(user),
                    EVENT_NOTIFICATION_ID,
                    buildConversationNotification(user, messages.sortedBy { it.timestampMs ?: 0L }),
                )
            }
            if (conversations.size > 1) {
                manager.notify(
                    groupKey,
                    SUMMARY_NOTIFICATION_ID,
                    buildSummaryNotification(AppNotification.Channel.PRIVATE_MESSAGES, notifications),
                )
            } else {
                manager.cancel(groupKey, SUMMARY_NOTIFICATION_ID)
            }
        }

        private suspend fun buildConversationNotification(
            user: String,
            messages: List<AppNotification>,
        ): Notification {
            val channel = AppNotification.Channel.PRIVATE_MESSAGES
            val tag = conversationTag(user)
            val avatar = loadAvatar(messages.lastOrNull()?.avatarUrl)
            val sender =
                Person
                    .Builder()
                    .setName(user)
                    .setKey(user)
                    .apply { avatar?.let { setIcon(IconCompat.createWithBitmap(it)) } }
                    .build()
            // Wlasna osoba wymagana przez MessagingStyle (etykieta nieistotna - nasze
            // wiadomosci nie sa tu pokazywane).
            val me = Person.Builder().setName(context.getString(R.string.conversation_me)).build()
            val style =
                NotificationCompat.MessagingStyle(me).also { style ->
                    messages.takeLast(CONVERSATION_MAX_MESSAGES).forEach { message ->
                        style.addMessage(message.message, message.timestampMs ?: 0L, sender)
                    }
                }

            val interopUrl = messages.last().interopUrl
            pushConversationShortcut(tag, user, sender, interopUrl)

            val allIdsV3 = messages.map { it.id }.toTypedArray()
            val allDismissed = messages.map { it.stableId() }.toLongArray()

            return NotificationCompat
                .Builder(context, channelId(channel))
                .setSmallIcon(R.drawable.ic_wykopmobilny)
                .setStyle(style)
                .setShortcutId(tag)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setGroup(groupKey(channel))
                .setAutoCancel(true)
                .apply {
                    avatar?.let(::setLargeIcon)
                    messages.last().timestampMs?.let {
                        setWhen(it)
                        setShowWhen(true)
                    }
                }
                // Tresc prywatnych wiadomosci nie moze byc widoczna na ekranie blokady -
                // wersja publiczna pokazuje tylko fakt otrzymania wiadomosci.
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicPmNotification(channel))
                .setDeleteIntent(dismissIntent(tag.hashCode(), allDismissed))
                .setContentIntent(contentIntent(tag.hashCode(), interopUrl))
                .addAction(
                    markReadAction(
                        channel = channel,
                        requestCode = tag.hashCode(),
                        notificationIds = allIdsV3,
                        cancelTag = tag,
                        dismissedIds = allDismissed,
                    ),
                ).addAction(replyAction(channel, tag, user, allIdsV3, allDismissed))
                .build()
        }

        private fun publicPmNotification(channel: AppNotification.Channel): Notification =
            NotificationCompat
                .Builder(context, channelId(channel))
                .setSmallIcon(R.drawable.ic_wykopmobilny)
                .setContentTitle(context.getString(R.string.channel_name_pm))
                .setContentText(context.getString(R.string.pm_public_content))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build()

        private fun pushConversationShortcut(
            tag: String,
            user: String,
            person: Person,
            interopUrl: String?,
        ) {
            runCatching {
                val intent =
                    interopIntentHandler(interopUrl)
                        .setAction(Intent.ACTION_VIEW)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ShortcutManagerCompat.pushDynamicShortcut(
                    context,
                    ShortcutInfoCompat
                        .Builder(context, tag)
                        .setShortLabel(user)
                        .setPerson(person)
                        .setLongLived(true)
                        .setIntent(intent)
                        .build(),
                )
            }.onFailure { Napier.w("Failed to push conversation shortcut", it) }
        }

        private fun conversationTag(user: String) = "pm:$user"

        // ==================== Wspolne klocki ====================

        private fun cancelStale(
            groupKey: String,
            currentTags: Set<String>,
        ) {
            manager.activeNotifications
                .filter { it.notification.group == groupKey }
                .filter { it.id == EVENT_NOTIFICATION_ID && it.tag !in currentTags }
                .forEach { manager.cancel(it.tag, EVENT_NOTIFICATION_ID) }
        }

        private fun contentIntent(
            requestCode: Int,
            interopUrl: String?,
        ): PendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                interopIntentHandler(interopUrl).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        // Awatar z CDN na potrzeby largeIcon/Person - synchronicznie na watku workera,
        // z twardym timeoutem Glide; blad/brak = po prostu bez awatara.
        private suspend fun loadAvatar(url: String?): Bitmap? {
            url?.takeIf { it.isNotBlank() } ?: return null
            return withContext(Dispatchers.IO) {
                runCatching {
                    runInterruptible {
                        Glide
                            .with(context)
                            .asBitmap()
                            .load(url)
                            .circleCrop()
                            .submit(AVATAR_SIZE_PX, AVATAR_SIZE_PX)
                            .get()
                    }
                }.getOrNull()
            }
        }

        private fun markReadAction(
            channel: AppNotification.Channel,
            requestCode: Int,
            notificationIds: Array<String>,
            cancelTag: String,
            dismissedIds: LongArray,
        ): NotificationCompat.Action {
            val intent =
                actionIntent(NotificationActionReceiver.ACTION_MARK_READ, channel)
                    .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_IDS, notificationIds)
                    .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_TAG, cancelTag)
                    .putExtra(NotificationActionReceiver.EXTRA_DISMISSED_IDS, dismissedIds)
            return NotificationCompat.Action
                .Builder(
                    R.drawable.ic_wykopmobilny,
                    context.getString(R.string.action_mark_read),
                    PendingIntent.getBroadcast(
                        context,
                        requestCode + REQUEST_OFFSET_MARK_READ,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                ).build()
        }

        private fun replyAction(
            channel: AppNotification.Channel,
            cancelTag: String,
            conversationUser: String,
            notificationIds: Array<String>,
            dismissedIds: LongArray,
        ): NotificationCompat.Action {
            val intent =
                actionIntent(NotificationActionReceiver.ACTION_REPLY, channel)
                    .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_IDS, notificationIds)
                    .putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_TAG, cancelTag)
                    .putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_USER, conversationUser)
                    .putExtra(NotificationActionReceiver.EXTRA_DISMISSED_IDS, dismissedIds)
            return NotificationCompat.Action
                .Builder(
                    R.drawable.ic_wykopmobilny,
                    context.getString(R.string.action_reply),
                    PendingIntent.getBroadcast(
                        context,
                        cancelTag.hashCode() + REQUEST_OFFSET_REPLY,
                        intent,
                        // RemoteInput wymaga MUTABLE - system dopisuje wynik do intencji.
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                ).addRemoteInput(
                    RemoteInput
                        .Builder(NotificationActionReceiver.REMOTE_INPUT_REPLY)
                        .setLabel(context.getString(R.string.action_reply))
                        .build(),
                ).setAllowGeneratedReplies(true)
                .build()
        }

        private fun buildSummaryNotification(
            channel: AppNotification.Channel,
            notifications: List<AppNotification>,
        ): Notification {
            val style =
                NotificationCompat.InboxStyle().also { style ->
                    notifications.take(SUMMARY_MAX_LINES).forEach { style.addLine(it.message) }
                    if (notifications.size > SUMMARY_MAX_LINES) {
                        style.setSummaryText("+${notifications.size - SUMMARY_MAX_LINES} ${context.getString(R.string.summary_more_notifications)}")
                    }
                }
            val allIds = notifications.map { it.stableId() }.toLongArray()
            return NotificationCompat
                .Builder(context, channelId(channel))
                .setSmallIcon(R.drawable.ic_wykopmobilny)
                .setContentTitle(channelName(channel))
                .setContentText(notifications.first().message)
                .setStyle(style)
                .setCategory(channel.category)
                .setGroup(groupKey(channel))
                .setGroupSummary(true)
                .setNumber(notifications.size)
                // Podsumowanie odswieza sie przy kazdym przebiegu workera - brzeczy tylko raz.
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .apply {
                    if (channel == AppNotification.Channel.PRIVATE_MESSAGES) {
                        setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        setPublicVersion(publicPmNotification(channel))
                    }
                }
                // Odrzucenie podsumowania zamyka cala grupe - zapamietaj wszystkie zdarzenia.
                .setDeleteIntent(dismissIntent(groupKey(channel).hashCode(), allIds))
                .setContentIntent(contentIntent(groupKey(channel).hashCode(), null))
                .addAction(
                    NotificationCompat.Action
                        .Builder(
                            R.drawable.ic_wykopmobilny,
                            context.getString(R.string.action_mark_all_read),
                            PendingIntent.getBroadcast(
                                context,
                                groupKey(channel).hashCode() + REQUEST_OFFSET_MARK_READ,
                                actionIntent(NotificationActionReceiver.ACTION_MARK_ALL_READ, channel)
                                    .putExtra(NotificationActionReceiver.EXTRA_DISMISSED_IDS, allIds),
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                            ),
                        ).build(),
                ).build()
        }

        private fun actionIntent(
            action: String,
            channel: AppNotification.Channel,
        ) = Intent(context, NotificationActionReceiver::class.java)
            .setAction(action)
            .putExtra(NotificationActionReceiver.EXTRA_CHANNEL, channel.name)

        private fun dismissIntent(
            requestCode: Int,
            dismissedIds: LongArray,
        ): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, NotificationDismissedReceiver::class.java)
                    .putExtra(NotificationDismissedReceiver.EXTRA_DISMISSED_IDS, dismissedIds),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun ensureChannels() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannelGroup(
                    NotificationChannelGroup(
                        NOTIFICATION_CHANNEL_GROUP_ID,
                        context.getString(R.string.channel_group_notifications),
                    ),
                )
                AppNotification.Channel.entries.forEach { channel ->
                    manager.createNotificationChannel(
                        NotificationChannel(channelId(channel), channelName(channel), channel.importance).apply {
                            group = NOTIFICATION_CHANNEL_GROUP_ID
                            setShowBadge(true)
                        },
                    )
                }
                // Stary pojedynczy kanal "general" zastapiony podzialem na typy.
                manager.deleteNotificationChannel(LEGACY_CHANNEL_GENERAL)
            }
        }

        private fun channelId(channel: AppNotification.Channel) = "$NOTIFICATION_CHANNEL_GROUP_ID:${channel.name.lowercase()}"

        private fun channelName(channel: AppNotification.Channel) =
            context.getString(
                when (channel) {
                    AppNotification.Channel.TO_ME -> R.string.channel_name_to_me
                    AppNotification.Channel.PRIVATE_MESSAGES -> R.string.channel_name_pm
                    AppNotification.Channel.TAGS -> R.string.channel_name_tags
                    AppNotification.Channel.OBSERVED_DISCUSSIONS -> R.string.channel_name_discussions
                },
            )

        private val AppNotification.Channel.importance: Int
            get() =
                when (this) {
                    // PM = heads-up; tagi celowo cicho (bywaja masowe).
                    AppNotification.Channel.PRIVATE_MESSAGES -> NotificationManager.IMPORTANCE_HIGH
                    AppNotification.Channel.TAGS -> NotificationManager.IMPORTANCE_LOW
                    else -> NotificationManager.IMPORTANCE_DEFAULT
                }

        private val AppNotification.Channel.category: String
            get() =
                when (this) {
                    AppNotification.Channel.PRIVATE_MESSAGES -> NotificationCompat.CATEGORY_MESSAGE
                    else -> NotificationCompat.CATEGORY_SOCIAL
                }

        companion object {
            private const val NOTIFICATION_CHANNEL_GROUP_ID = "io.github.wykopmobilny:notifications"
            private const val LEGACY_CHANNEL_GENERAL = "$NOTIFICATION_CHANNEL_GROUP_ID:general"

            // Rozrozniane po (tag, id): zdarzenia maja tag=id-zdarzenia (PM: "pm:{user}"),
            // podsumowania tag=grupa.
            internal const val EVENT_NOTIFICATION_ID = 124
            internal const val SUMMARY_NOTIFICATION_ID = 125
            private const val SUMMARY_MAX_LINES = 5
            private const val CONVERSATION_MAX_MESSAGES = 8
            private const val AVATAR_SIZE_PX = 128
            private const val REQUEST_OFFSET_MARK_READ = 1_000_000
            private const val REQUEST_OFFSET_REPLY = 2_000_000

            internal fun groupKey(channel: AppNotification.Channel) = "group:${channel.name.lowercase()}"
        }
    }

internal class NotificationDismissedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val dismissedIds = intent?.getLongArrayExtra(EXTRA_DISMISSED_IDS)?.toList().orEmpty()
        if (dismissedIds.isEmpty()) return
        val dependencies = context.requireDependency<NotificationDependencies>()
        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                dependencies.handleNotificationDismissed().invoke(dismissedIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_DISMISSED_IDS = "dismissed_ids"
    }
}

// Akcje z powiadomien: mark-as-read / mark-all / direct reply. Siec przez goAsync
// (limit ~10s wystarcza na pojedyncze calle; runBlocking na main = ryzyko ANR).
internal class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        intent ?: return
        val channel =
            intent
                .getStringExtra(EXTRA_CHANNEL)
                ?.let { name -> AppNotification.Channel.entries.firstOrNull { it.name == name } }
                ?: return
        val dependencies = context.requireDependency<NotificationDependencies>()
        val manager = context.getSystemService<NotificationManager>() ?: return
        val dismissedIds = intent.getLongArrayExtra(EXTRA_DISMISSED_IDS)?.toList().orEmpty()
        val notificationIds = intent.getStringArrayExtra(EXTRA_NOTIFICATION_IDS)?.toList().orEmpty()
        val cancelTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                when (intent.action) {
                    ACTION_MARK_READ -> {
                        notificationIds.forEach { id ->
                            runCatching { dependencies.markNotificationRead().invoke(channel, id) }
                                .onFailure { Napier.w("mark-read failed", it) }
                        }
                        dependencies.handleNotificationDismissed().invoke(dismissedIds)
                        cancelTag?.let { manager.cancel(it, AndroidNotificationManager.EVENT_NOTIFICATION_ID) }
                        cancelSummaryIfGroupEmpty(manager, channel)
                    }

                    ACTION_MARK_ALL_READ -> {
                        runCatching { dependencies.markChannelRead().invoke(channel) }
                            .onFailure { Napier.w("mark-all-read failed", it) }
                        dependencies.handleNotificationDismissed().invoke(dismissedIds)
                        // Zamknij cala grupe (zdarzenia + podsumowanie).
                        val groupKey = AndroidNotificationManager.groupKey(channel)
                        manager.activeNotifications
                            .filter { it.notification.group == groupKey }
                            .forEach { manager.cancel(it.tag, it.id) }
                    }

                    ACTION_REPLY -> {
                        val user = intent.getStringExtra(EXTRA_CONVERSATION_USER) ?: return@launch
                        val text =
                            RemoteInput
                                .getResultsFromIntent(intent)
                                ?.getCharSequence(REMOTE_INPUT_REPLY)
                                ?.toString()
                                .orEmpty()
                        if (text.isBlank()) return@launch
                        val result = runCatching { dependencies.sendPrivateMessageReply().invoke(user, text) }
                        if (result.isSuccess) {
                            notificationIds.forEach { id ->
                                runCatching { dependencies.markNotificationRead().invoke(channel, id) }
                            }
                            dependencies.handleNotificationDismissed().invoke(dismissedIds)
                            // Anulowanie powiadomienia konczy tez spinner direct reply.
                            cancelTag?.let { manager.cancel(it, AndroidNotificationManager.EVENT_NOTIFICATION_ID) }
                            cancelSummaryIfGroupEmpty(manager, channel)
                        } else {
                            Napier.w("pm reply failed", result.exceptionOrNull())
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, R.string.reply_failed, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun cancelSummaryIfGroupEmpty(
        manager: NotificationManager,
        channel: AppNotification.Channel,
    ) {
        val groupKey = AndroidNotificationManager.groupKey(channel)
        val eventsLeft =
            manager.activeNotifications.any {
                it.notification.group == groupKey && it.id == AndroidNotificationManager.EVENT_NOTIFICATION_ID
            }
        if (!eventsLeft) manager.cancel(groupKey, AndroidNotificationManager.SUMMARY_NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_MARK_READ = "io.github.wykopmobilny.notification.MARK_READ"
        const val ACTION_MARK_ALL_READ = "io.github.wykopmobilny.notification.MARK_ALL_READ"
        const val ACTION_REPLY = "io.github.wykopmobilny.notification.REPLY"
        const val EXTRA_CHANNEL = "channel"
        const val EXTRA_NOTIFICATION_IDS = "notification_ids"
        const val EXTRA_NOTIFICATION_TAG = "notification_tag"
        const val EXTRA_CONVERSATION_USER = "conversation_user"
        const val EXTRA_DISMISSED_IDS = "dismissed_ids"
        const val REMOTE_INPUT_REPLY = "reply_text"
    }
}

// Wspolny scope receiverow powiadomien (goAsync) - IO, niezalezny od lifecycle'u.
private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

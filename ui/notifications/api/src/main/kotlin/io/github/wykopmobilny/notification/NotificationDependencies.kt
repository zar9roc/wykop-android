package io.github.wykopmobilny.notification

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface NotificationDependencies {
    fun handleNotificationDismissed(): HandleNotificationDismissed

    fun markNotificationRead(): MarkNotificationRead

    fun markChannelRead(): MarkChannelRead

    fun sendPrivateMessageReply(): SendPrivateMessageReply

    fun refreshNotifications(): RefreshNotifications

    fun frequentPollingInterval(): FrequentPollingInterval
}

interface HandleNotificationDismissed {
    // Uzytkownik odrzucil powiadomienia o podanych id (stableNotificationId) - zapamietaj,
    // zeby kolejne odswiezenia nie pokazywaly ich ponownie.
    suspend operator fun invoke(dismissedIds: List<Long>)
}

// Akcja "Oznacz jako przeczytane" na pojedynczym zdarzeniu (PUT w API v3).
interface MarkNotificationRead {
    suspend operator fun invoke(
        channel: AppNotification.Channel,
        notificationId: String,
    )
}

// Akcja "Oznacz wszystkie jako przeczytane" na podsumowaniu kanalu.
interface MarkChannelRead {
    suspend operator fun invoke(channel: AppNotification.Channel)
}

// Direct reply z powiadomienia PM - wysyla wiadomosc do rozmowcy.
interface SendPrivateMessageReply {
    suspend operator fun invoke(
        username: String,
        content: String,
    )
}

// Odswiezenie powiadomien i publikacja do shade. statusFirst = najpierw tani
// GET /notifications/status, listy kanalow pobierane tylko przy niezerowych licznikach
// (tryb foreground pollingu); false = bezwarunkowy pelny przebieg (worker 15-min).
interface RefreshNotifications {
    suspend operator fun invoke(statusFirst: Boolean)
}

// Interwal czestego sprawdzania (foreground service): null = tryb wylaczony
// (powiadomienia wylaczone albo wybrany okres >= 15 min obslugiwany WorkManagerem).
interface FrequentPollingInterval {
    operator fun invoke(): Flow<Duration?>
}

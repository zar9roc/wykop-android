package io.github.wykopmobilny.notification

interface NotificationsManager {
    /**
     * Publikuje PELNY aktualny stan kanalu: kazde zdarzenie jako osobne powiadomienie
     * (tag = id zdarzenia) + podsumowanie grupy. Zdarzenia pokazane wczesniej, a nieobecne
     * na liscie, sa anulowane; pusta lista czysci caly kanal.
     */
    suspend fun publish(
        channel: AppNotification.Channel,
        notifications: List<AppNotification>,
    )
}

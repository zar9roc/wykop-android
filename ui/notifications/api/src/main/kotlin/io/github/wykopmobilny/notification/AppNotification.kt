package io.github.wykopmobilny.notification

/**
 * Pojedyncze zdarzenie-powiadomienie (per wpis/wiadomosc/tag), publikowane jako osobne
 * powiadomienie systemowe w kanale [channel], grupowane per kanal z podsumowaniem.
 */
data class AppNotification(
    // Id zdarzenia z API v3 (krotki string, np. "VWG9zLpL") - unikalne per zdarzenie,
    // uzywane jako tag powiadomienia i (po hashu) klucz dismissed-trackingu.
    val id: String,
    val title: String,
    val message: String,
    // Czas zdarzenia (ms epoch) - setWhen; null = bez znacznika czasu.
    val timestampMs: Long?,
    val channel: Channel,
    // Deep-link (format rozumiany przez WykopLinkHandler); null = lista powiadomien.
    val interopUrl: String?,
    // Login rozmowcy (tylko PRIVATE_MESSAGES) - umozliwia akcje "Odpowiedz" (direct reply)
    // oraz styl konwersacji (MessagingStyle + skrot -> sekcja "Konwersacje" Android 11+).
    val conversationUser: String? = null,
    // Awatar autora zdarzenia (CDN) - largeIcon / ikona Person; null = bez awatara.
    val avatarUrl: String? = null,
) {
    // Kanaly powiadomien (uzytkownik steruje kazdym osobno w ustawieniach systemowych).
    enum class Channel {
        TO_ME,
        PRIVATE_MESSAGES,
        TAGS,
        OBSERVED_DISCUSSIONS,
    }
}

/** Stabilny 64-bitowy hash id zdarzenia - klucz tabeli dismissed (schema wymaga Long). */
fun AppNotification.stableId(): Long = stableNotificationId(id)

fun stableNotificationId(id: String): Long {
    // FNV-1a 64-bit - deterministyczny miedzy uruchomieniami (hashCode() by wystarczyl,
    // ale 32 bity to niepotrzebnie wieksze ryzyko kolizji w tabeli).
    var hash = -0x340d631b7bdddcdbL
    for (ch in id) {
        hash = hash xor ch.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash
}

package io.github.wykopmobilny.models.dataclass

data class PMMessage(
    // Identyfikator wiadomosci z API (pole "key") - uzywany jako prev_message/next_message
    // przy dociaganiu starszych/nowszych wiadomosci.
    val key: String?,
    val date: String,
    val body: String,
    val embed: Embed?,
    val isSentFromUser: Boolean,
    val app: String?,
    // Pole "read" z API - dla własnych wiadomości oznacza odczytanie przez rozmówcę.
    val isRead: Boolean = false,
)

package io.github.wykopmobilny.models.dataclass

data class PMMessage(
    val date: String,
    val body: String,
    val embed: Embed?,
    val isSentFromUser: Boolean,
    val app: String?,
    // Pole "read" z API - dla własnych wiadomości oznacza odczytanie przez rozmówcę.
    val isRead: Boolean = false,
)

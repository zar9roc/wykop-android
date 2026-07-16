package io.github.wykopmobilny.models.dataclass

data class Conversation(
    val user: Author,
    val lastUpdate: String,
    // Nieprzeczytana rozmowa (pogrubienie na liście) + zajawka ostatniej
    // wiadomości - oba pola przychodzą w tej samej odpowiedzi /pm/conversations.
    val unread: Boolean = false,
    val lastMessagePreview: String? = null,
)

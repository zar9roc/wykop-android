package io.github.wykopmobilny.models.dataclass

data class FullConversation(
    val messages: List<PMMessage>,
    val receiver: Author,
    // Zielony znacznik aktywnosci rozmowcy (pole "online" z obiektu user).
    val online: Boolean = false,
)

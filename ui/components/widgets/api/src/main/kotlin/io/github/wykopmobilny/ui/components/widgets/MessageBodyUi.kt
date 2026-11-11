package io.github.wykopmobilny.ui.components.widgets

data class MessageBodyUi(
    val content: CharSequence?,
    val maxLines: Int,
    val viewMoreAction: () -> Unit,
    /** Tapniecie w tresc poza linkami (np. podglad tresci usunietego komentarza); null = brak reakcji. */
    val clickAction: (() -> Unit)? = null,
)

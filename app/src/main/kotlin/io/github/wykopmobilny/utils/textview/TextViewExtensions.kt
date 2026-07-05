package io.github.wykopmobilny.utils.textview

import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.widget.TextView
import io.github.wykopmobilny.kotlin.linkifyTagsAndMentions

fun TextView.prepareBody(
    html: String,
    listener: (String) -> Unit,
) {
    text = SpannableStringBuilder(html.linkifyTagsAndMentions().toSpannable())
    val method = BetterLinkMovementMethod.linkifyHtml(this)
    method.setOnLinkClickListener { _, url ->
        if (url.span() is URLSpan) {
            listener.invoke(url.text())
            true
        } else {
            // Niestandardowe spany (np. spoiler) obsluguja klikniecie same - ich
            // "tekst" to nie URL i otwieranie go w przegladarce konczy sie bledem.
            false
        }
    }
}

fun TextView.prepareBody(
    html: String,
    urlClickListener: (String) -> Unit,
    clickListener: (() -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") openSpoilersDialog: Boolean,
) {
    text = SpannableStringBuilder(html.linkifyTagsAndMentions().toSpannable())
    val method = BetterLinkMovementMethod.linkifyHtml(this)
    clickListener?.let {
        method.setOnTextClickListener {
            clickListener()
        }
    }
    method.setOnLinkClickListener { _, url ->
        if (url.span() is URLSpan) {
            urlClickListener(url.text())
            true
        } else {
            false
        }
    }
}

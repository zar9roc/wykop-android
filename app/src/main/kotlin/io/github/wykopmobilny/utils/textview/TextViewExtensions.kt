package io.github.wykopmobilny.utils.textview

import android.text.SpannableStringBuilder
import android.widget.TextView

fun TextView.prepareBody(
    html: String,
    listener: (String) -> Unit,
) {
    text = SpannableStringBuilder(html.linkifyTagsAndMentions().toSpannable())
    val method = BetterLinkMovementMethod.linkifyHtml(this)
    method.setOnLinkClickListener { _, url ->
        listener.invoke(url.text())
        true
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
        urlClickListener(url.text())
        true
    }
}

package io.github.wykopmobilny.utils.textview

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

/**
 * A clickable span that shows/hides spoiler content when clicked.
 * Initially displays "[pokaż spoiler]", expands to full content on click.
 */
class SpoilerClickableSpan(
    private val spoilerContent: String,
) : ClickableSpan() {
    private var isExpanded = false

    override fun onClick(widget: View) {
        if (widget !is TextView) return

        isExpanded = !isExpanded

        // Get the current text as SpannableStringBuilder
        val spannable = SpannableStringBuilder(widget.text)

        // Find this span in the spannable
        val spanStart = spannable.getSpanStart(this)
        val spanEnd = spannable.getSpanEnd(this)

        if (spanStart >= 0 && spanEnd >= 0) {
            // Replace the text at this span's position
            val newText = if (isExpanded) spoilerContent else COLLAPSED_TEXT
            spannable.replace(spanStart, spanEnd, newText)

            // Re-apply this span to the new text
            val newEnd = spanStart + newText.length
            spannable.setSpan(this, spanStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Update the TextView
            widget.text = spannable
        }
    }

    override fun updateDrawState(ds: android.text.TextPaint) {
        super.updateDrawState(ds)
        // Keep the default link styling (blue, underlined)
    }

    companion object {
        /**
         * The text displayed when spoiler is collapsed.
         */
        const val COLLAPSED_TEXT = "[pokaż spoiler]"
    }
}

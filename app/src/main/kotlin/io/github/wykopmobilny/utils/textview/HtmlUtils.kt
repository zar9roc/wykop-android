package io.github.wykopmobilny.utils.textview

import android.text.Spannable
import android.text.Spanned
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.toSpannable

fun String.toSpannable(): Spannable =
    HtmlCompat
        .fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT, null, CodeTagHandler())
        .toSpannable()
        .restyleQuotes()
        .apply {
            // Styl spoilera dokladany PO parsowaniu - LineHeightSpan to ParagraphStyle,
            // a Html.fromHtml wymaga od takich spanow granic akapitu (crash w trakcie).
            getSpans<SpoilerClickableSpan>().forEach { spoiler ->
                SpoilerClickableSpan.applyStyling(this, getSpanStart(spoiler), getSpanEnd(spoiler))
            }
        }

fun String.removeHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

fun String.stripWykopFormatting(): String = removeHtml()

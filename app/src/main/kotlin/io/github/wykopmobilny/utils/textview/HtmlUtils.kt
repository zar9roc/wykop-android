package io.github.wykopmobilny.utils.textview

import android.text.Spannable
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable

fun String.toSpannable(openSpoilerInDialog: Boolean = false): Spannable =
    HtmlCompat
        .fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT, null, CodeTagHandler(openSpoilerInDialog))
        .toSpannable()
        .restyleQuotes()
    // Zwinieta etykieta "[pokaż spoiler]" celowo BEZ monospace/odstepow - ma wygladac
    // jak zwykly link (np. "[pokaż całość]"). Styl monospace dokladany jest dopiero do
    // rozwinietej TRESCI w SpoilerClickableSpan.expandInline().

fun String.removeHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

fun String.stripWykopFormatting(): String = removeHtml()

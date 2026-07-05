package io.github.wykopmobilny.utils.textview

import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.LineHeightSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.TextView

/**
 * Klikalna zaslona spoilera - "[pokaż spoiler]" wyglada jak link, a klikniecie
 * JEDNORAZOWO podmienia zaslone na tresc (bez zwijania z powrotem).
 *
 * Tresc trzymana jest jako CharSequence ZE spanami - linki/tagi/wzmianki wewnatrz
 * spoilera pozostaja klikalne po rozwinieciu, a tekst zachowuje sie jak zwykly
 * (mozna go zaznaczac/kopiowac). Tresc dostaje monospace + odstepy przez
 * [applyStyling]; te same style zdejmowane sa z zaslony przed podmiana.
 */
class SpoilerClickableSpan(
    private val spoilerContent: CharSequence,
) : ClickableSpan() {
    override fun onClick(widget: View) {
        if (widget !is TextView) return

        val spannable = SpannableStringBuilder(widget.text)
        val spanStart = spannable.getSpanStart(this)
        val spanEnd = spannable.getSpanEnd(this)

        if (spanStart >= 0 && spanEnd >= 0) {
            spannable
                .getSpans(spanStart, spanEnd, SpoilerPaddingSpan::class.java)
                .forEach(spannable::removeSpan)
            spannable
                .getSpans(spanStart, spanEnd, SpoilerTypefaceSpan::class.java)
                .forEach(spannable::removeSpan)
            spannable.removeSpan(this)

            // Podmiana zaslony na tresc - replace() kopiuje spany zrodla (Spanned),
            // wiec linki wewnatrz spoilera laduja w tekscie jako klikalne URLSpany.
            spannable.replace(spanStart, spanEnd, spoilerContent)
            applyStyling(spannable, spanStart, spanStart + spoilerContent.length)

            widget.text = spannable
        }
    }

    companion object {
        /**
         * The text displayed when spoiler is collapsed.
         */
        const val COLLAPSED_TEXT = "[pokaż spoiler]"

        /**
         * Aplikuje styl spoilera (monospace + odstepy) na podany zakres.
         */
        fun applyStyling(
            spannable: Spannable,
            start: Int,
            end: Int,
        ) {
            spannable.setSpan(SpoilerTypefaceSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(SpoilerPaddingSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

/**
 * Monospace dla tresci spoilera - musi byc MetricAffectingSpan (TypefaceSpan),
 * zeby lamanie linii liczylo sie dla faktycznej szerokosci znakow.
 */
class SpoilerTypefaceSpan : TypefaceSpan("monospace")

/**
 * Niewielki odstep nad pierwsza i pod ostatnia linia spoilera.
 */
class SpoilerPaddingSpan : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: Paint.FontMetricsInt,
    ) {
        val spanned = text as Spanned
        val spanStart = spanned.getSpanStart(this)
        val spanEnd = spanned.getSpanEnd(this)
        val padding = (fm.descent - fm.ascent) / PADDING_LINE_FRACTION

        if (spanStart in start until end) {
            fm.ascent -= padding
            fm.top -= padding
        }
        if (spanEnd in (start + 1)..end) {
            fm.descent += padding
            fm.bottom += padding
        }
    }

    companion object {
        // Odstep = 1/4 wysokosci linii - skaluje sie z rozmiarem czcionki.
        private const val PADDING_LINE_FRACTION = 4
    }
}

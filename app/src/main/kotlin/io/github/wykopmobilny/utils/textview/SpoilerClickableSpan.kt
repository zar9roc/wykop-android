package io.github.wykopmobilny.utils.textview

import android.graphics.Paint
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.LineHeightSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import io.github.wykopmobilny.R

/**
 * Klikalna zaslona spoilera - "[pokaż spoiler]" wyglada jak link. Klikniecie:
 * - [openInDialog] = false (domyslnie): JEDNORAZOWO podmienia zaslone na tresc
 *   inline (bez zwijania z powrotem);
 * - [openInDialog] = true: pokazuje tresc w okienku (AlertDialog) - zachowanie
 *   sterowane ustawieniem "Otwieraj spoilery w okienku".
 *
 * Tresc trzymana jest jako CharSequence ZE spanami - linki/tagi/wzmianki wewnatrz
 * spoilera pozostaja klikalne po rozwinieciu, a tekst zachowuje sie jak zwykly
 * (mozna go zaznaczac/kopiowac). Tresc dostaje monospace + odstepy przez
 * [applyStyling]; te same style zdejmowane sa z zaslony przed podmiana.
 */
class SpoilerClickableSpan(
    private val spoilerContent: CharSequence,
    private val openInDialog: Boolean = false,
) : ClickableSpan() {
    override fun onClick(widget: View) {
        if (widget !is TextView) return
        if (openInDialog) {
            showInDialog(widget)
        } else {
            expandInline(widget)
        }
    }

    private fun showInDialog(widget: TextView) {
        val dialog =
            AlertDialog
                .Builder(widget.context)
                .setTitle(R.string.spoiler_dialog_title)
                .setMessage(SpannableStringBuilder(spoilerContent))
                .setPositiveButton(android.R.string.ok, null)
                .create()
        dialog.show()
        // Linki wewnatrz spoilera maja byc klikalne rowniez w okienku.
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun expandInline(widget: TextView) {
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

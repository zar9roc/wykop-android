package io.github.wykopmobilny.domain.navigation.android

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.widget.TextView

/**
 * Zaslona spoilera w stacku v3 (szczegoly linku / komentarze linkow). "[pokaż spoiler]"
 * wyglada jak link. Klik:
 * - [openInDialog] = false (domyslnie): podmienia zaslone na tresc inline (jednorazowo);
 * - [openInDialog] = true: wola [onDialog] z trescia - domena pokazuje ja w okienku
 *   (InfoDialogUi), zgodnie z ustawieniem "Otwieraj spoilery w okienku".
 *
 * Mirror logiki z app/utils/textview/SpoilerClickableSpan.kt - moduly nie wspoldziela
 * androidowego modulu util (patrz komentarz przy restyleQuotes w AndroidWykopTextUtils).
 */
internal const val SPOILER_COLLAPSED_TEXT = "[pokaż spoiler]"

internal class SpoilerSpan(
    private val content: CharSequence,
    private val openInDialog: Boolean,
    private val onDialog: (CharSequence) -> Unit,
) : ClickableSpan() {
    override fun onClick(widget: View) {
        if (openInDialog) {
            onDialog(content)
            return
        }
        if (widget !is TextView) return

        val spannable = SpannableStringBuilder(widget.text)
        val start = spannable.getSpanStart(this)
        val end = spannable.getSpanEnd(this)
        if (start < 0 || end < 0) return

        spannable.getSpans(start, end, SpoilerMonospaceSpan::class.java).forEach(spannable::removeSpan)
        spannable.removeSpan(this)
        // replace() kopiuje spany zrodla - linki wewnatrz spoilera pozostaja klikalne.
        spannable.replace(start, end, content)
        spannable.setSpan(SpoilerMonospaceSpan(), start, start + content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        widget.text = spannable
    }
}

internal class SpoilerMonospaceSpan : TypefaceSpan("monospace")

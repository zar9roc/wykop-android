package io.github.wykopmobilny.utils.textview

import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.TypefaceSpan
import org.xml.sax.XMLReader

class CodeTagHandler : Html.TagHandler {
    override fun handleTag(
        opening: Boolean,
        tag: String?,
        output: Editable?,
        reader: XMLReader?,
    ) {
        when {
            tag.equals("code", true) -> handleCodeTag(opening, output)
            tag.equals("spoiler", true) -> handleSpoilerTag(opening, output)
        }
    }

    private fun handleCodeTag(
        opening: Boolean,
        output: Editable?,
    ) {
        val len = output?.length
        if (opening) {
            output?.setSpan(TypefaceSpan("monospace"), len!!, len, Spannable.SPAN_MARK_MARK)
        } else {
            val obj = getLast(output!!, CharacterStyle::class.java)
            obj?.let {
                val where = output.getSpanStart(obj)

                if (obj is TypefaceSpan) {
                    output.setSpan(TypefaceSpan("monospace"), where, len!!, 0)
                }
            }
        }
    }

    private fun handleSpoilerTag(
        opening: Boolean,
        output: Editable?,
    ) {
        if (output == null) return

        val len = output.length
        if (opening) {
            // Mark the start position
            output.setSpan(SpoilerMarker(), len, len, Spannable.SPAN_MARK_MARK)
        } else {
            // Find the marker we placed at the opening tag
            val marker = getLast(output, SpoilerMarker::class.java) as? SpoilerMarker
            marker?.let {
                val start = output.getSpanStart(marker)

                // Extract the spoiler content - jako CharSequence ZE spanami,
                // zeby linki wewnatrz spoilera pozostaly klikalne po rozwinieciu.
                val spoilerContent = output.subSequence(start, len)

                // Create the clickable span
                val spoilerSpan = SpoilerClickableSpan(spoilerContent)

                // Replace content with collapsed text
                output.replace(start, len, SpoilerClickableSpan.COLLAPSED_TEXT)

                // Apply the clickable span
                val newEnd = start + SpoilerClickableSpan.COLLAPSED_TEXT.length
                output.setSpan(spoilerSpan, start, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                // Remove the marker
                output.removeSpan(marker)
            }
        }
    }

    private fun getLast(
        text: Editable,
        kind: Class<*>,
    ): Any? {
        val objs = text.getSpans(0, text.length, kind)

        return if (objs.isEmpty()) {
            null
        } else {
            (objs.size downTo 1)
                .firstOrNull { text.getSpanFlags(objs[it - 1]) == Spannable.SPAN_MARK_MARK }
                ?.let { objs[it - 1] }
        }
    }

    /**
     * Internal marker class to track spoiler tag positions during HTML parsing.
     */
    private class SpoilerMarker
}

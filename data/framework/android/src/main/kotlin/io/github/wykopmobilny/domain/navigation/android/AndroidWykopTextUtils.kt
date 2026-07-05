package io.github.wykopmobilny.domain.navigation.android

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.toSpannable
import io.github.wykopmobilny.domain.navigation.WykopTextUtils
import io.github.wykopmobilny.domain.navigation.WykopTextUtils.RecognizedLink
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.XMLReader
import javax.inject.Inject

internal class AndroidWykopTextUtils
    @Inject
    constructor() : WykopTextUtils {
        override suspend fun parseHtml(
            text: String,
            onLinkClicked: ((RecognizedLink) -> Unit)?,
        ): CharSequence =
            withContext(AppDispatchers.Default) {
                val parsed =
                    HtmlCompat
                        .fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT, null, CodeTagHandler())
                        .toSpannable()
                        .restyleQuotes()

                if (onLinkClicked == null) {
                    parsed
                } else {
                    parsed.apply {
                        getSpans<URLSpan>().forEach { span ->
                            setSpan(
                                clickableSpan {
                                    val url = span.url
                                    val link =
                                        when {
                                            url.startsWith("@") -> RecognizedLink.Profile(url.substringAfter("@"))
                                            url.startsWith("#") -> RecognizedLink.Tag(url.substringAfter("#"))
                                            url.startsWith("spoiler:") -> RecognizedLink.Spoiler(id = url.substringAfter("spoiler:"))
                                            else -> RecognizedLink.Other(url)
                                        }
                                    onLinkClicked(link)
                                },
                                getSpanStart(span),
                                getSpanEnd(span),
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                            )
                            removeSpan(span)
                        }
                    }
                }
            }
    }

inline fun clickableSpan(crossinline onClick: () -> Unit) =
    object : ClickableSpan() {
        override fun onClick(widget: View) = onClick()
    }

/**
 * Podmienia domyslne, ledwo widoczne QuoteSpany z Html.fromHtml na wyrazniejszy
 * styl cytatu: szeroka szara belka + kursywa.
 *
 * UWAGA: kopia tej logiki zyje w app/utils/textview/QuoteStyling.kt -
 * moduly nie wspoldziela zadnego androidowego modulu utili.
 */
private fun Spannable.restyleQuotes(): Spannable {
    getSpans<QuoteSpan>().forEach { quote ->
        val start = getSpanStart(quote)
        val end = getSpanEnd(quote)
        val flags = getSpanFlags(quote)
        removeSpan(quote)
        setSpan(WykopQuoteSpan(), start, end, flags)
        setSpan(StyleSpan(Typeface.ITALIC), start, end, flags)
    }
    return this
}

private class WykopQuoteSpan : LeadingMarginSpan {
    override fun getLeadingMargin(first: Boolean): Int = STRIPE_WIDTH_PX + GAP_WIDTH_PX

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout,
    ) {
        val previousStyle = p.style
        val previousColor = p.color
        p.style = Paint.Style.FILL
        p.color = STRIPE_COLOR
        c.drawRect(x.toFloat(), top.toFloat(), (x + dir * STRIPE_WIDTH_PX).toFloat(), bottom.toFloat(), p)
        p.style = previousStyle
        p.color = previousColor
    }

    companion object {
        private const val STRIPE_WIDTH_PX = 8
        private const val GAP_WIDTH_PX = 24
        private const val STRIPE_COLOR = 0xFF9E9E9E.toInt()
    }
}

private class CodeTagHandler : Html.TagHandler {
    override fun handleTag(
        opening: Boolean,
        tag: String,
        output: Editable,
        reader: XMLReader,
    ) {
        if (tag.equals("code", true)) {
            val len = output.length
            if (opening) {
                output.setSpan(TypefaceSpan("monospace"), len, len, Spannable.SPAN_MARK_MARK)
            } else {
                val obj = findClosing<CharacterStyle>(output)
                obj?.let {
                    val where = output.getSpanStart(obj)

                    if (obj is TypefaceSpan) {
                        output.setSpan(TypefaceSpan("monospace"), where, len, 0)
                    }
                }
            }
        }
    }

    private inline fun <reified T : Any> findClosing(text: Editable): Any? {
        val spans = text.getSpans<T>()

        return if (spans.isEmpty()) {
            null
        } else {
            (spans.size downTo 1)
                .firstOrNull { text.getSpanFlags(spans[it - 1]) == Spannable.SPAN_MARK_MARK }
                ?.let { spans[it - 1] }
        }
    }
}

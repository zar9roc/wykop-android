package io.github.wykopmobilny.utils.textview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.LeadingMarginSpan
import android.text.style.QuoteSpan
import android.text.style.StyleSpan
import androidx.core.text.getSpans

/**
 * Podmienia domyslne, ledwo widoczne QuoteSpany z Html.fromHtml na wyrazniejszy
 * styl cytatu: szeroka szara belka + kursywa.
 *
 * UWAGA: kopia tej logiki zyje w AndroidWykopTextUtils (data/framework/android) -
 * moduly nie wspoldziela zadnego androidowego modulu utili.
 */
fun Spannable.restyleQuotes(): Spannable {
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

/**
 * Belka cytatu jak QuoteSpan, ale o kontrolowanej szerokosci i kolorze
 * (3-argumentowy konstruktor QuoteSpan wymaga API 28, minSdk = 24).
 */
class WykopQuoteSpan : LeadingMarginSpan {
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
        layout: android.text.Layout,
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
        // Px zamiast dp - spany nie maja kontekstu; przy gestosciach 2-3x daje ~3-4dp belki.
        private const val STRIPE_WIDTH_PX = 8
        private const val GAP_WIDTH_PX = 24
        private const val STRIPE_COLOR = 0xFF9E9E9E.toInt()
    }
}

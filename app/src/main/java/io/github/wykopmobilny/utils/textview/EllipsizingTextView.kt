package io.github.wykopmobilny.utils.textview

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextUtils
import android.text.TextUtils.TruncateAt
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import java.util.regex.Pattern

/**
 * A [android.widget.TextView] that ellipsizes more intelligently.
 * This class supports ellipsizing multiline text through setting `android:ellipsize`
 * and `android:maxLines`.
 *
 * Note: [android.text.TextUtils.TruncateAt.MARQUEE] ellipsizing type is not supported.
 */
class EllipsizingTextView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle,
    ) : AppCompatTextView(context, attrs, defStyleAttr) {
        private val ellipsisText = SpannableString("\u0020[pokaż\u00A0całość]")
        private val endPunctPattern: Pattern = DEFAULT_END_PUNCTUATION

        private var ellipsizeStrategy: EllipsizeStrategy = EllipsizeNoneStrategy()
        var isEllipsized: Boolean = false
            private set
        private var isStale: Boolean = false
        private var programmaticChange: Boolean = false
        private var fullText: CharSequence? = null
        private var maxLinesCount: Int = Integer.MAX_VALUE
        private var lineSpacingMult: Float = 1.0f
        private var lineAddVertPad: Float = 0.0f

        init {
            val typedArray: TypedArray =
                context.obtainStyledAttributes(
                    attrs,
                    intArrayOf(android.R.attr.maxLines, android.R.attr.ellipsize),
                    defStyleAttr,
                    0,
                )
            setMaxLines(typedArray.getInt(0, Integer.MAX_VALUE))
            typedArray.recycle()

            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(androidx.appcompat.R.attr.colorAccent, typedValue, true)
            @ColorInt val color = typedValue.data
            ellipsisText.setSpan(
                ForegroundColorSpan(color),
                0,
                ellipsisText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        override fun getMaxLines(): Int = maxLinesCount

        override fun setMaxLines(maxLines: Int) {
            super.setMaxLines(maxLines)
            this.maxLinesCount = maxLines
            isStale = true
        }

        /**
         * Determines if the last fully visible line is being ellipsized.
         *
         * @return `true` if the last fully visible line is being ellipsized;
         * otherwise, returns `false`.
         */
        fun ellipsizingLastFullyVisibleLine(): Boolean = maxLinesCount == Integer.MAX_VALUE

        override fun setLineSpacing(
            add: Float,
            mult: Float,
        ) {
            lineAddVertPad = add
            lineSpacingMult = mult
            super.setLineSpacing(add, mult)
        }

        override fun setText(
            text: CharSequence?,
            type: BufferType?,
        ) {
            if (!programmaticChange) {
                fullText = if (text is Spanned) text else text
                isStale = true
            }
            super.setText(text, type)
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (ellipsizingLastFullyVisibleLine()) {
                isStale = true
            }
        }

        override fun setPadding(
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            super.setPadding(left, top, right, bottom)
            if (ellipsizingLastFullyVisibleLine()) {
                isStale = true
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (isStale) {
                resetText()
            }
            super.onDraw(canvas)
        }

        /**
         * Sets the ellipsized text if appropriate.
         */
        private fun resetText() {
            val maxLines = getMaxLines()
            var workingText: CharSequence? = fullText
            var ellipsized = false

            if (maxLines != -1) {
                if (ellipsizeStrategy == null) {
                    setEllipsize(null)
                }
                workingText = ellipsizeStrategy.processText(fullText)
                ellipsized = !ellipsizeStrategy.isInLayout(fullText)
            }

            if (workingText != getText()) {
                programmaticChange = true
                try {
                    setText(workingText)
                } finally {
                    programmaticChange = false
                }
            }

            isStale = false
            if (ellipsized != isEllipsized) {
                isEllipsized = ellipsized
            }
        }

        /**
         * Causes words in the text that are longer than the view is wide to be ellipsized
         * instead of broken in the middle. Use `null` to turn off ellipsizing.
         *
         * Note: Method does nothing for [android.text.TextUtils.TruncateAt.MARQUEE]
         * ellipsizing type.
         *
         * @param where part of text to ellipsize
         */
        override fun setEllipsize(where: TruncateAt?) {
            ellipsizeStrategy =
                when (where) {
                    TruncateAt.END -> EllipsizeEndStrategy()
                    TruncateAt.START -> EllipsizeStartStrategy()
                    TruncateAt.MIDDLE -> EllipsizeMiddleStrategy()
                    TruncateAt.MARQUEE, null -> EllipsizeNoneStrategy()
                }
        }

        /**
         * A base class for an ellipsize strategy.
         */
        private abstract inner class EllipsizeStrategy {
            /**
             * Returns ellipsized text if the text does not fit inside of the layout;
             * otherwise, returns the full text.
             *
             * @param text text to process
             * @return Ellipsized text if the text does not fit inside of the layout;
             * otherwise, returns the full text.
             */
            fun processText(text: CharSequence?): CharSequence? =
                if (text != null && !isInLayout(text)) {
                    createEllipsizedText(text)
                } else {
                    text
                }

            /**
             * Determines if the text fits inside of the layout.
             *
             * @param text text to fit
             * @return `true` if the text fits inside of the layout;
             * otherwise, returns `false`.
             */
            fun isInLayout(text: CharSequence?): Boolean {
                if (text == null) return true
                val layout = createWorkingLayout(text)
                return layout.lineCount <= getLinesCount()
            }

            /**
             * Creates a working layout with the given text.
             *
             * @param workingText text to create layout with
             * @return [android.text.Layout] with the given text.
             */
            protected fun createWorkingLayout(workingText: CharSequence): Layout =
                StaticLayout.Builder
                    .obtain(
                        workingText,
                        0,
                        workingText.length,
                        paint,
                        width - compoundPaddingLeft - compoundPaddingRight,
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(lineAddVertPad, lineSpacingMult)
                    .setIncludePad(false)
                    .build()

            /**
             * Get how many lines of text we are allowed to display.
             */
            protected fun getLinesCount(): Int =
                if (ellipsizingLastFullyVisibleLine()) {
                    val fullyVisibleLinesCount = getFullyVisibleLinesCount()
                    if (fullyVisibleLinesCount == -1) 1 else fullyVisibleLinesCount
                } else {
                    maxLinesCount
                }

            /**
             * Get how many lines of text we can display so their full height is visible.
             */
            protected fun getFullyVisibleLinesCount(): Int {
                val layout = createWorkingLayout("")
                val height = height - compoundPaddingTop - compoundPaddingBottom
                val lineHeight = layout.getLineBottom(0)
                return height / lineHeight
            }

            /**
             * Creates ellipsized text from the given text.
             *
             * @param fullText text to ellipsize
             * @return Ellipsized text
             */
            protected abstract fun createEllipsizedText(fullText: CharSequence): CharSequence
        }

        /**
         * An [EllipsizeStrategy] that does not ellipsize text.
         */
        private inner class EllipsizeNoneStrategy : EllipsizeStrategy() {
            override fun createEllipsizedText(fullText: CharSequence): CharSequence = fullText
        }

        /**
         * An [EllipsizeStrategy] that ellipsizes text at the end.
         */
        private inner class EllipsizeEndStrategy : EllipsizeStrategy() {
            override fun createEllipsizedText(fullText: CharSequence): CharSequence {
                val layout = createWorkingLayout(fullText)
                val cutOffIndex = layout.getLineEnd(maxLinesCount - 1)
                val textLength = fullText.length
                var cutOffLength = textLength - cutOffIndex
                if (cutOffLength < ellipsisText.length) {
                    cutOffLength = ellipsisText.length
                }
                var workingText: CharSequence =
                    TextUtils
                        .substring(
                            fullText,
                            0,
                            textLength - cutOffLength,
                        ).trim()

                while (!isInLayout(TextUtils.concat(stripEndPunctuation(workingText), ellipsisText))) {
                    val lastSpace = TextUtils.lastIndexOf(workingText, ' ')
                    if (lastSpace == -1) {
                        break
                    }
                    workingText = TextUtils.substring(workingText, 0, lastSpace).trim()
                }

                workingText = TextUtils.concat(stripEndPunctuation(workingText), ellipsisText)
                val dest = SpannableStringBuilder(workingText)

                if (fullText is Spanned) {
                    TextUtils.copySpansFrom(
                        fullText,
                        0,
                        workingText.length - 15,
                        null,
                        dest,
                        0,
                    )
                }
                return dest
            }

            /**
             * Strips the end punctuation from a given text according to [endPunctPattern].
             *
             * @param workingText text to strip end punctuation from
             * @return Text without end punctuation.
             */
            fun stripEndPunctuation(workingText: CharSequence): String = endPunctPattern.matcher(workingText).replaceFirst("")
        }

        /**
         * An [EllipsizeStrategy] that ellipsizes text at the start.
         */
        private inner class EllipsizeStartStrategy : EllipsizeStrategy() {
            override fun createEllipsizedText(fullText: CharSequence): CharSequence {
                val layout = createWorkingLayout(fullText)
                val cutOffIndex = layout.getLineEnd(maxLinesCount - 1)
                val textLength = fullText.length
                var cutOffLength = textLength - cutOffIndex
                if (cutOffLength < ellipsisText.length) {
                    cutOffLength = ellipsisText.length
                }
                var workingText: CharSequence =
                    TextUtils
                        .substring(
                            fullText,
                            cutOffLength,
                            textLength,
                        ).trim()

                while (!isInLayout(TextUtils.concat(ellipsisText, workingText))) {
                    val firstSpace = TextUtils.indexOf(workingText, ' ')
                    if (firstSpace == -1) {
                        break
                    }
                    workingText = TextUtils.substring(workingText, firstSpace, workingText.length).trim()
                }

                workingText = TextUtils.concat(ellipsisText, workingText)
                val dest = SpannableStringBuilder(workingText)

                if (fullText is Spanned) {
                    TextUtils.copySpansFrom(
                        fullText,
                        textLength - workingText.length,
                        textLength,
                        null,
                        dest,
                        0,
                    )
                }
                return dest
            }
        }

        /**
         * An [EllipsizeStrategy] that ellipsizes text in the middle.
         */
        private inner class EllipsizeMiddleStrategy : EllipsizeStrategy() {
            override fun createEllipsizedText(fullText: CharSequence): CharSequence {
                val layout = createWorkingLayout(fullText)
                val cutOffIndex = layout.getLineEnd(maxLinesCount - 1)
                val textLength = fullText.length
                var cutOffLength = textLength - cutOffIndex
                if (cutOffLength < ellipsisText.length) {
                    cutOffLength = ellipsisText.length
                }
                cutOffLength += cutOffIndex % 2 // Make it even.
                var firstPart =
                    TextUtils
                        .substring(
                            fullText,
                            0,
                            textLength / 2 - cutOffLength / 2,
                        ).trim()
                var secondPart =
                    TextUtils
                        .substring(
                            fullText,
                            textLength / 2 + cutOffLength / 2,
                            textLength,
                        ).trim()

                while (!isInLayout(TextUtils.concat(firstPart, ellipsisText, secondPart))) {
                    val lastSpaceFirstPart = firstPart.lastIndexOf(' ')
                    val firstSpaceSecondPart = secondPart.indexOf(' ')
                    if (lastSpaceFirstPart == -1 || firstSpaceSecondPart == -1) {
                        break
                    }
                    firstPart = firstPart.substring(0, lastSpaceFirstPart).trim()
                    secondPart = secondPart.substring(firstSpaceSecondPart).trim()
                }

                val firstDest = SpannableStringBuilder(firstPart)
                val secondDest = SpannableStringBuilder(secondPart)

                if (fullText is Spanned) {
                    TextUtils.copySpansFrom(fullText, 0, firstPart.length, null, firstDest, 0)
                    TextUtils.copySpansFrom(
                        fullText,
                        textLength - secondPart.length,
                        textLength,
                        null,
                        secondDest,
                        0,
                    )
                }
                return TextUtils.concat(firstDest, ellipsisText, secondDest)
            }
        }

        companion object {
            const val MAX_LINES = 8
            private val DEFAULT_END_PUNCTUATION = Pattern.compile("[.!?,;:…]*$", Pattern.DOTALL)
        }
    }

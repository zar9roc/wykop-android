package io.github.wykopmobilny.ui.widgets.buttons

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import androidx.core.content.ContextCompat
import io.github.wykopmobilny.R
import io.github.wykopmobilny.utils.getActivityContext

class MinusVoteButton
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.MirkoButtonStyle,
    ) : VoteButton(context, attrs, defStyleAttr) {
        init {
            val typedValue = TypedValue()
            getActivityContext()!!.theme?.resolveAttribute(R.attr.voteMinusButtonStatelist, typedValue, true)
            setBackgroundResource(typedValue.resourceId)

            // Create ColorStateList programmatically to support theme colors
            val textColorTypedValue = TypedValue()
            val resolvedTextColor = context.theme.resolveAttribute(R.attr.textColorButtonToolbar, textColorTypedValue, true)
            val defaultColor =
                if (resolvedTextColor && textColorTypedValue.data != 0) {
                    textColorTypedValue.data
                } else {
                    // Fallback to colorControlNormal if textColorButtonToolbar is not defined
                    context.theme.resolveAttribute(androidx.appcompat.R.attr.colorControlNormal, textColorTypedValue, true)
                    textColorTypedValue.data
                }
            val selectedColor = ContextCompat.getColor(context, R.color.minusPressedColor)

            val states =
                arrayOf(
                    intArrayOf(android.R.attr.state_selected),
                    intArrayOf(),
                )
            val colors = intArrayOf(selectedColor, defaultColor)
            setTextColor(ColorStateList(states, colors))
        }

        override fun setLightThemeDrawable() {
            if (isSelected) {
                setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_buttontoolbar_minus_activ, 0, 0, 0)
            } else {
                val typedArray =
                    context.obtainStyledAttributes(
                        arrayOf(
                            R.attr.minusDrawable,
                        ).toIntArray(),
                    )
                setCompoundDrawablesWithIntrinsicBounds(typedArray.getDrawable(0), null, null, null)
                typedArray.recycle()
            }
        }
    }

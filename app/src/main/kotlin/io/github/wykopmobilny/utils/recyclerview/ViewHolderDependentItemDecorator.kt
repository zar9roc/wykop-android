package io.github.wykopmobilny.utils.recyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.ui.adapters.viewholders.RecyclableViewHolder

class ViewHolderDependentItemDecorator(
    val context: Context,
) : RecyclerView.ItemDecoration() {
    val paint by lazy { Paint() }

    // Separator sekcji (wpis + jego komentarze): delikatna przerwa, ten sam kształt
    // w każdym motywie - różni się wyłącznie kolorem z ?attr/sectionSeparatorColor,
    // więc w amoledzie wychodzi naturalnie czarna przerwa.
    private val sectionPaint by lazy { Paint() }
    private val largeHeight by lazy { context.resources.getDimension(R.dimen.separator_large).toInt() }
    private val normalHeight by lazy { context.resources.getDimension(R.dimen.separator_normal).toInt() }
    private val sectionHeight by lazy { context.resources.getDimension(R.dimen.separator_section).toInt() }

    init {
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(R.attr.lineColor, typedValue, true)
        paint.style = Paint.Style.FILL
        paint.color = typedValue.data

        val sectionValue = TypedValue()
        theme.resolveAttribute(R.attr.sectionSeparatorColor, sectionValue, true)
        sectionPaint.style = Paint.Style.FILL
        sectionPaint.color = sectionValue.data
    }

    private fun heightFor(view: View) =
        when (view.tag) {
            RecyclableViewHolder.SEPARATOR_SECTION -> sectionHeight
            RecyclableViewHolder.SEPARATOR_NORMAL -> largeHeight
            else -> normalHeight
        }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        @Suppress("TooGenericExceptionCaught") // Defensive: RecyclerView state exceptions
        try {
            outRect.set(0, 0, 0, heightFor(view))
        } catch (exception: Exception) {
            Napier.w("Couldn't get item offset", exception)
        }
    }

    override fun onDraw(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        try {
            super.onDraw(c, parent, state)
            for (i in 0 until parent.childCount) {
                val view = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(view)
                if (position > -1 && parent.adapter != null && parent.adapter!!.itemCount >= position) {
                    val isSection = view.tag == RecyclableViewHolder.SEPARATOR_SECTION
                    c.drawRect(
                        view.left.toFloat(),
                        view.bottom.toFloat(),
                        view.right.toFloat(),
                        (view.bottom + heightFor(view)).toFloat(),
                        if (isSection) sectionPaint else paint,
                    )
                }
            }
        } catch (_: Exception) {
            // Do nothing
        }
    }
}

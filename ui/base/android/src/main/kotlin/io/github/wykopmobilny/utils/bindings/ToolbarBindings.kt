package io.github.wykopmobilny.utils.bindings

import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar

fun Toolbar.bindBackButton(activity: ComponentActivity?) {
    val typedValue = TypedValue()
    val resolved = context.theme.resolveAttribute(android.R.attr.homeAsUpIndicator, typedValue, true)
    if (resolved) {
        setNavigationIcon(typedValue.resourceId)
    }
    setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
}

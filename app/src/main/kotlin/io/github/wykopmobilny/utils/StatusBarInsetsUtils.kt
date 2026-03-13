package io.github.wykopmobilny.utils

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Ustawia gorny padding toolbara na rzeczywista wysokosc status bar
 * uzywajac WindowInsetsCompat API zamiast stalej wartosci 24dp.
 */
fun Toolbar.applyStatusBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.setPadding(
            view.paddingLeft,
            statusBarInsets.top,
            view.paddingRight,
            view.paddingBottom,
        )
        windowInsets
    }
}

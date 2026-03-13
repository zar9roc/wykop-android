package io.github.wykopmobilny.utils

import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Ustawia gorny padding toolbara na rzeczywista wysokosc status bar
 * uzywajac WindowInsetsCompat API zamiast stalej wartosci 24dp.
 *
 * Wymaga android:fitsSystemWindows="true" na toolbarze w XML,
 * aby view uczestniczyl w dystrybucji window insets.
 * Listener nadpisuje domyslne zachowanie fitsSystemWindows
 * i aplikuje wylacznie gorny inset status bar.
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
    ViewCompat.requestApplyInsets(this)
}

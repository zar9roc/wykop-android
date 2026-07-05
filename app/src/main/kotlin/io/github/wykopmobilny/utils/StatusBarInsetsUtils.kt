package io.github.wykopmobilny.utils

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.github.wykopmobilny.R
import io.github.wykopmobilny.debug.DiagnosticCheckpoint

/**
 * Ustawia gorny padding widoku na rzeczywista wysokosc status bar
 * uzywajac WindowInsetsCompat API zamiast stalej wartosci 24dp.
 *
 * Toolbar wymaga android:fitsSystemWindows="true" w XML,
 * aby view uczestniczyl w dystrybucji window insets.
 * Listener nadpisuje domyslne zachowanie fitsSystemWindows
 * (oraz wewnetrzny listener NavigationView/ScrimInsetsFrameLayout)
 * i aplikuje wylacznie gorny inset status bar.
 */
fun View.applyStatusBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
        view.setPadding(
            view.paddingLeft,
            statusBarInsets.top,
            view.paddingRight,
            view.paddingBottom,
        )
        DiagnosticCheckpoint.log(
            "StatusBarInsets",
            "${view.javaClass.simpleName} paddingTop=${statusBarInsets.top}px applied",
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Aplikuje inset status bar na toolbarach tworzonych w layoutach fragmentow
 * (np. LinkDetailsFragment). Toolbary z layoutu activity obsluguje
 * onContentChanged() w klasach bazowych - fragmenty tworza swoje widoki
 * pozniej, wiec wymagaja osobnego callbacku.
 */
fun AppCompatActivity.applyStatusBarInsetsToFragmentToolbars() {
    supportFragmentManager.registerFragmentLifecycleCallbacks(
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager,
                f: Fragment,
                v: View,
                savedInstanceState: Bundle?,
            ) {
                v.findViewById<Toolbar>(R.id.toolbar)?.applyStatusBarInsets()
            }
        },
        true,
    )
}

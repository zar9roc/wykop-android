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
 * Dosuwa content aktywnosci nad klawiature ekranowa.
 *
 * Od targetSdk 35 (wymuszony edge-to-edge) manifestowe adjustResize jest ignorowane -
 * okno nie zmniejsza sie po otwarciu IME i klawiatura rysuje sie NA polu odpowiedzi.
 * Listener na android.R.id.content odtwarza zachowanie adjustResize: dolny padding
 * rowny insetowi IME. Gdy klawiatura jest schowana, inset wynosi 0.
 */
fun AppCompatActivity.applyImeInsetsToContent() {
    val content = findViewById<View>(android.R.id.content) ?: return
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
        val imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        if (view.paddingBottom != imeBottom) {
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, imeBottom)
            DiagnosticCheckpoint.log("ImeInsets", "content paddingBottom=${imeBottom}px")
        }
        windowInsets
    }
    ViewCompat.requestApplyInsets(content)
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

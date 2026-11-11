package io.github.wykopmobilny.ui.modules.photoview

import android.app.Activity
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible

/**
 * "Chrome" pelnoekranowego podgladu mediow (toolbar + paski systemowe) chowany
 * pojedynczym tapnieciem - jak w typowych galeriach. Po ukryciu paski systemowe
 * mozna chwilowo wyciagnac gestem od krawedzi (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE).
 */
internal class MediaChromeController(
    activity: Activity,
    private val toolbar: View,
) {
    private val insetsController =
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

    var isVisible: Boolean = true
        private set

    fun toggle() = setVisible(!isVisible)

    fun setVisible(visible: Boolean) {
        if (visible == isVisible) return
        isVisible = visible
        toolbar.animate().cancel()
        if (visible) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            toolbar.isVisible = true
            toolbar
                .animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION_MS)
                .start()
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            toolbar
                .animate()
                .alpha(0f)
                .translationY(-toolbar.height.toFloat())
                .setDuration(ANIMATION_DURATION_MS)
                .withEndAction { toolbar.isVisible = false }
                .start()
        }
    }
}

private const val ANIMATION_DURATION_MS = 200L

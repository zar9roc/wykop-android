package io.github.wykopmobilny.initializers

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.debug.DebugActivityTracker

/**
 * Registers debug-only tools like [DebugActivityTracker].
 * Initialized via AndroidX Startup in the debug manifest.
 */
internal class DebugToolsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(DebugActivityTracker)
        Napier.d("DebugActivityTracker registered", tag = "DebugToolsInitializer")
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

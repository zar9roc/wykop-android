package io.github.wykopmobilny.initializers

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.debug.DebugActivityTracker
import io.github.wykopmobilny.debug.DebugHttpServer

/**
 * Registers debug-only tools like [DebugActivityTracker] and [DebugHttpServer].
 * Initialized via AndroidX Startup in the debug manifest.
 */
internal class DebugToolsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(DebugActivityTracker)
        Napier.d("DebugActivityTracker registered", tag = "DebugToolsInitializer")

        val server = DebugHttpServer(context.applicationContext)
        server.start()
        Napier.d("DebugHttpServer started", tag = "DebugToolsInitializer")
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

package io.github.wykopmobilny.initializers

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.debug.DebugActivityTracker
import io.github.wykopmobilny.debug.DebugServerHolder
import io.github.wykopmobilny.debug.LeakCanaryToggle
import io.github.wykopmobilny.WykopApp

/**
 * Registers debug-only tools like [DebugActivityTracker] and DebugHttpServer.
 * Initialized via AndroidX Startup in the debug manifest.
 */
internal class DebugToolsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val app = context.applicationContext as Application
        app.registerActivityLifecycleCallbacks(DebugActivityTracker)
        Napier.d("DebugActivityTracker registered", tag = "DebugToolsInitializer")

        val wykopApp = context.applicationContext as WykopApp
        val entriesApi = wykopApp.wykopApi.entriesV3RetrofitApi()
        val linksApi = wykopApp.wykopApi.linksV3RetrofitApi()
        DebugServerHolder.initialize(context.applicationContext, entriesApi, linksApi)
        Napier.d("DebugHttpServer initialized via DebugServerHolder", tag = "DebugToolsInitializer")

        LeakCanaryToggle.apply(context)
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

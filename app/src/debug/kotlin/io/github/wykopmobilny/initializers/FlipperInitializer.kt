package io.github.wykopmobilny.initializers

import android.content.Context
import androidx.startup.Initializer
import com.facebook.flipper.android.AndroidFlipperClient
import com.facebook.flipper.android.utils.FlipperUtils
import com.facebook.flipper.plugins.databases.DatabasesFlipperPlugin
import com.facebook.flipper.plugins.inspector.DescriptorMapping
import com.facebook.flipper.plugins.inspector.InspectorFlipperPlugin
import com.facebook.flipper.plugins.leakcanary2.FlipperLeakEventListener
import com.facebook.flipper.plugins.leakcanary2.LeakCanary2FlipperPlugin
import com.facebook.flipper.plugins.network.NetworkFlipperPlugin
import com.facebook.flipper.plugins.sharedpreferences.SharedPreferencesFlipperPlugin
import com.facebook.soloader.SoLoader
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.debug.FlipperPluginHolder
import leakcanary.LeakCanary

internal class FlipperInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        SoLoader.init(context, false)

        if (FlipperUtils.shouldEnableFlipper(context)) {
            val client = AndroidFlipperClient.getInstance(context)

            // Network plugin - stored in holder for use in RetrofitModule
            val networkPlugin = NetworkFlipperPlugin()
            FlipperPluginHolder.networkPlugin = networkPlugin
            client.addPlugin(networkPlugin)

            // Layout Inspector
            client.addPlugin(InspectorFlipperPlugin(context, DescriptorMapping.withDefaults()))

            // SharedPreferences
            client.addPlugin(SharedPreferencesFlipperPlugin(context))

            // Database - SQLDelight support
            client.addPlugin(DatabasesFlipperPlugin(context))

            // LeakCanary integration
            val leakCanaryPlugin = LeakCanary2FlipperPlugin()
            client.addPlugin(leakCanaryPlugin)
            LeakCanary.config =
                LeakCanary.config.copy(
                    eventListeners = LeakCanary.config.eventListeners + FlipperLeakEventListener(),
                )

            client.start()

            Napier.d("Flipper initialized successfully", tag = "FlipperInitializer")
        }
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

package io.github.wykopmobilny.initializers

import android.content.Context
import androidx.startup.Initializer
import io.github.aakira.napier.Napier

internal class LoggingInitializer : Initializer<Napier> {
    override fun create(context: Context): Napier {
        val antilog = FileLogAntilog(context)
        Napier.base(antilog)
        antilog.installAsUncaughtExceptionHandler()

        return Napier
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}

package io.github.wykopmobilny.debug

import android.content.Context
import io.github.aakira.napier.Napier
import leakcanary.LeakCanary

/**
 * Debug-only przelacznik heap dumpow LeakCanary.
 *
 * Automatyzacja E2E (adb) wylacza dumpy, bo heads-up "dumping heap" przechwytuje
 * tapniecia i rozjezdza scenariusze testowe. Stan trzymany w SharedPreferences,
 * wiec przezywa restarty procesu i reinstalacje (`adb install -r`).
 *
 * Sterowanie:
 *   adb shell am broadcast -n io.github.wykopmobilny.debug/.DebugStateReceiver \
 *     -a io.github.wykopmobilny.debug.DEBUG_LEAKCANARY --ez enabled false
 */
object LeakCanaryToggle {
    private const val PREFS = "debug_tools"
    private const val KEY_ENABLED = "leakcanary_heap_dump_enabled"

    fun apply(context: Context) {
        val enabled = isEnabled(context)
        LeakCanary.config = LeakCanary.config.copy(dumpHeap = enabled)
        Napier.d("LeakCanary heap dump enabled=$enabled", tag = "DebugState")
    }

    fun set(
        context: Context,
        enabled: Boolean,
    ) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        apply(context)
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

package io.github.wykopmobilny.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only BroadcastReceiver that dumps structured app state as JSON to logcat
 * and provides control actions for testing. Delegates to [DebugStateHelper].
 *
 * Actions:
 *   DEBUG_STATE — dump app state to logcat (default)
 *   DEBUG_CLEAR_CACHE — clear app cache directory
 *   DEBUG_LOGOUT — force logout current user
 *   DEBUG_SWITCH_TAB — switch to specific tab in MainNavigationActivity
 *   DEBUG_LEAKCANARY — enable/disable LeakCanary heap dumps (--ez enabled true|false)
 *
 * Usage examples:
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_LOGOUT
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab "promoted"
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_LEAKCANARY --ez enabled false
 *   adb logcat -s DebugState -d | tail -1
 *
 * Optional extras for DEBUG_STATE:
 *   --ez verbose true   — include fragment back stack and device info
 *
 * Tab names for DEBUG_SWITCH_TAB:
 *   promoted, upcoming, hits, hot, mywykop, favorite, search, messages, notifications
 */
class DebugStateReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DebugState"
        const val ACTION_DEBUG_STATE = "io.github.wykopmobilny.debug.DEBUG_STATE"
        const val ACTION_CLEAR_CACHE = "io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE"
        const val ACTION_LOGOUT = "io.github.wykopmobilny.debug.DEBUG_LOGOUT"
        const val ACTION_SWITCH_TAB = "io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB"
        const val ACTION_LEAKCANARY = "io.github.wykopmobilny.debug.DEBUG_LEAKCANARY"

        @Deprecated("Use ACTION_DEBUG_STATE")
        const val ACTION = ACTION_DEBUG_STATE
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_DEBUG_STATE -> {
                val verbose = intent.getBooleanExtra("verbose", false)
                val state = DebugStateHelper.buildState(context, verbose)
                Napier.i(state.toString(2), tag = TAG)
            }

            ACTION_CLEAR_CACHE -> {
                val result = DebugStateHelper.clearCache(context)
                Napier.i(result.toString(2), tag = TAG)
            }

            ACTION_LOGOUT -> {
                scope.launch {
                    val result = DebugStateHelper.forceLogout(context)
                    Napier.i(result.toString(2), tag = TAG)
                }
            }

            ACTION_LEAKCANARY -> {
                val enabled = intent.getBooleanExtra("enabled", true)
                LeakCanaryToggle.set(context, enabled)
                Napier.i("""{"action": "leakcanary", "heap_dump_enabled": $enabled}""", tag = TAG)
            }

            ACTION_SWITCH_TAB -> {
                val tab = intent.getStringExtra("tab")
                if (tab != null) {
                    val result = DebugStateHelper.switchTab(context, tab)
                    Napier.i(result.toString(2), tag = TAG)
                } else {
                    Napier.w("SWITCH_TAB requires --es tab parameter", tag = TAG)
                }
            }

            else -> {
                Napier.w("Unknown action: ${intent.action}", tag = TAG)
            }
        }
    }
}

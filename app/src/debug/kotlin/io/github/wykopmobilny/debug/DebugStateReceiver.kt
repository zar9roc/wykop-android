package io.github.wykopmobilny.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only BroadcastReceiver that dumps structured app state as JSON to logcat
 * and provides control actions for testing.
 *
 * Actions:
 *   DEBUG_STATE — dump app state to logcat (default)
 *   DEBUG_CLEAR_CACHE — clear app cache directory
 *   DEBUG_LOGOUT — force logout current user
 *   DEBUG_SWITCH_TAB — switch to specific tab in MainNavigationActivity
 *
 * Usage examples:
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_CLEAR_CACHE
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_LOGOUT
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_SWITCH_TAB --es tab "promoted"
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

        // For backward compatibility
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
                val state = buildState(context, verbose)
                Napier.i(state.toString(2), tag = TAG)
            }

            ACTION_CLEAR_CACHE -> {
                clearCache(context)
            }

            ACTION_LOGOUT -> {
                forceLogout(context)
            }

            ACTION_SWITCH_TAB -> {
                val tab = intent.getStringExtra("tab")
                if (tab != null) {
                    switchTab(context, tab)
                } else {
                    Napier.w("SWITCH_TAB requires --es tab parameter", tag = TAG)
                }
            }

            else -> {
                Napier.w("Unknown action: ${intent.action}", tag = TAG)
            }
        }
    }

    private fun buildState(
        context: Context,
        verbose: Boolean,
    ): JSONObject {
        val json = JSONObject()
        val activity = DebugActivityTracker.currentActivity

        // Activity info
        json.put("activity", activity?.javaClass?.simpleName ?: "none (app in background)")
        json.put("activity_class", activity?.javaClass?.name ?: JSONObject.NULL)

        // Fragment info
        if (activity is FragmentActivity) {
            val fm = activity.supportFragmentManager
            val fragments = fm.fragments
            val visibleFragments = fragments.filter { it.isVisible }

            json.put("fragment", visibleFragments.firstOrNull()?.javaClass?.simpleName ?: "none")
            json.put(
                "all_fragments",
                JSONArray(
                    fragments.map { f ->
                        JSONObject().apply {
                            put("name", f.javaClass.simpleName)
                            put("visible", f.isVisible)
                            put("resumed", f.isResumed)
                        }
                    },
                ),
            )

            if (verbose) {
                json.put("back_stack_count", fm.backStackEntryCount)
                val backStack = JSONArray()
                for (i in 0 until fm.backStackEntryCount) {
                    backStack.put(fm.getBackStackEntryAt(i).name ?: "unnamed")
                }
                json.put("back_stack", backStack)
            }

            // Check for nested fragments (e.g. ViewPager tabs)
            visibleFragments.firstOrNull()?.let { topFragment ->
                val childFragments = topFragment.childFragmentManager.fragments
                if (childFragments.isNotEmpty()) {
                    val visibleChildren = childFragments.filter { it.isVisible }
                    json.put(
                        "child_fragment",
                        visibleChildren.firstOrNull()?.javaClass?.simpleName ?: "none",
                    )
                    json.put(
                        "child_fragments",
                        JSONArray(
                            childFragments.map { f ->
                                JSONObject().apply {
                                    put("name", f.javaClass.simpleName)
                                    put("visible", f.isVisible)
                                }
                            },
                        ),
                    )
                }
            }
        }

        // User state
        val app = context.applicationContext as? WykopApp
        if (app != null) {
            val userManager = app.userManagerApi.get()
            val credentials = userManager.getUserCredentials()
            json.put("user_logged_in", userManager.isUserAuthorized())
            json.put("user_login", credentials?.login ?: JSONObject.NULL)
        }

        // App info
        json.put("package", context.packageName)
        json.put("version_name", getVersionName(context))

        if (verbose) {
            json.put(
                "device",
                JSONObject().apply {
                    put("model", Build.MODEL)
                    put("sdk", Build.VERSION.SDK_INT)
                    put("manufacturer", Build.MANUFACTURER)
                },
            )
        }

        return json
    }

    private fun getVersionName(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }

    private fun clearCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val filesDeleted = deleteRecursive(cacheDir)
            val result =
                JSONObject().apply {
                    put("action", "clear_cache")
                    put("success", true)
                    put("files_deleted", filesDeleted)
                    put("cache_path", cacheDir.absolutePath)
                }
            Napier.i(result.toString(2), tag = TAG)
        } catch (e: Exception) {
            val error =
                JSONObject().apply {
                    put("action", "clear_cache")
                    put("success", false)
                    put("error", e.message)
                }
            Napier.w(error.toString(2), tag = TAG)
        }
    }

    private fun deleteRecursive(fileOrDirectory: java.io.File): Int {
        var count = 0
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                count += deleteRecursive(child)
            }
        }
        if (fileOrDirectory.delete()) {
            count++
        }
        return count
    }

    private fun forceLogout(context: Context) {
        scope.launch {
            try {
                val app = context.applicationContext as? WykopApp
                if (app != null) {
                    val userManager = app.userManagerApi.get()
                    val wasLoggedIn = userManager.isUserAuthorized()
                    userManager.logoutUser()
                    val result =
                        JSONObject().apply {
                            put("action", "logout")
                            put("success", true)
                            put("was_logged_in", wasLoggedIn)
                        }
                    Napier.i(result.toString(2), tag = TAG)
                } else {
                    throw IllegalStateException("WykopApp not available")
                }
            } catch (e: Exception) {
                val error =
                    JSONObject().apply {
                        put("action", "logout")
                        put("success", false)
                        put("error", e.message)
                    }
                Napier.w(error.toString(2), tag = TAG)
            }
        }
    }

    private fun switchTab(
        context: Context,
        tab: String,
    ) {
        try {
            val fragmentKey =
                when (tab.lowercase()) {
                    "promoted", "home" -> MainNavigationActivity.TARGET_NOTIFICATIONS

                    // Will be handled as default
                    "notifications" -> MainNavigationActivity.TARGET_NOTIFICATIONS

                    else -> null
                }

            val intent =
                if (fragmentKey != null) {
                    MainNavigationActivity.getIntent(context, fragmentKey)
                } else {
                    // For other tabs, we'll need to use a custom approach
                    // since MainNavigationActivity only has TARGET_NOTIFICATIONS constant
                    Intent(context, MainNavigationActivity::class.java).apply {
                        putExtra("debug_target_tab", tab)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            context.startActivity(intent)

            val result =
                JSONObject().apply {
                    put("action", "switch_tab")
                    put("success", true)
                    put("tab", tab)
                    put("note", "Tab switch requested - check if MainNavigationActivity is in foreground")
                }
            Napier.i(result.toString(2), tag = TAG)
        } catch (e: Exception) {
            val error =
                JSONObject().apply {
                    put("action", "switch_tab")
                    put("success", false)
                    put("tab", tab)
                    put("error", e.message)
                }
            Napier.w(error.toString(2), tag = TAG)
        }
    }
}

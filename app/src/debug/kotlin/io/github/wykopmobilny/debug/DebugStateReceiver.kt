package io.github.wykopmobilny.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only BroadcastReceiver that dumps structured app state as JSON to logcat.
 *
 * Usage:
 *   adb shell am broadcast -a io.github.wykopmobilny.debug.DEBUG_STATE
 *   adb logcat -s DebugState -d | tail -1
 *
 * Optional extras:
 *   --ez verbose true   — include fragment back stack and device info
 */
class DebugStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DebugState"
        const val ACTION = "io.github.wykopmobilny.debug.DEBUG_STATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val verbose = intent.getBooleanExtra("verbose", false)
        val state = buildState(context, verbose)
        Napier.i(state.toString(2), tag = TAG)
    }

    private fun buildState(context: Context, verbose: Boolean): JSONObject {
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
                JSONArray(fragments.map { f ->
                    JSONObject().apply {
                        put("name", f.javaClass.simpleName)
                        put("visible", f.isVisible)
                        put("resumed", f.isResumed)
                    }
                }),
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
                        JSONArray(childFragments.map { f ->
                            JSONObject().apply {
                                put("name", f.javaClass.simpleName)
                                put("visible", f.isVisible)
                            }
                        }),
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
}

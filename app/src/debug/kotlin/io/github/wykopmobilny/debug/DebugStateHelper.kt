package io.github.wykopmobilny.debug

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.ui.modules.links.linkdetails.LinkDetailsActivityV2
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import io.github.wykopmobilny.ui.modules.mikroblog.entry.EntryActivity
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Shared debug helper used by both [DebugStateReceiver] and [DebugHttpServer].
 * All methods are safe to call from any thread unless noted otherwise.
 */
object DebugStateHelper {

    /**
     * Build app state JSON. Must be called on main thread (accesses Activity/Fragment).
     */
    fun buildState(context: Context, verbose: Boolean): JSONObject {
        val json = JSONObject()
        val activity = DebugActivityTracker.currentActivity

        json.put("activity", activity?.javaClass?.simpleName ?: "none (app in background)")
        json.put("activity_class", activity?.javaClass?.name ?: JSONObject.NULL)

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

        val app = context.applicationContext as? WykopApp
        if (app != null) {
            val userManager = app.userManagerApi.get()
            val credentials = userManager.getUserCredentials()
            json.put("user_logged_in", userManager.isUserAuthorized())
            json.put("user_login", credentials?.login ?: JSONObject.NULL)
        }

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

    /**
     * Clear app cache. Safe to call from any thread.
     */
    fun clearCache(context: Context): JSONObject {
        return try {
            val cacheDir = context.cacheDir
            val filesDeleted = deleteRecursive(cacheDir)
            JSONObject().apply {
                put("action", "clear_cache")
                put("success", true)
                put("files_deleted", filesDeleted)
                put("cache_path", cacheDir.absolutePath)
            }
        } catch (e: IOException) {
            JSONObject().apply {
                put("action", "clear_cache")
                put("success", false)
                put("error", e.message)
            }
        }
    }

    /**
     * Force logout. Must be called on main thread (UserManager may touch UI).
     */
    suspend fun forceLogout(context: Context): JSONObject {
        return try {
            val app = context.applicationContext as? WykopApp
                ?: return JSONObject().apply {
                    put("action", "logout")
                    put("success", false)
                    put("error", "WykopApp not available")
                }

            val userManager = app.userManagerApi.get()
            val wasLoggedIn = userManager.isUserAuthorized()
            userManager.logoutUser()
            JSONObject().apply {
                put("action", "logout")
                put("success", true)
                put("was_logged_in", wasLoggedIn)
            }
        } catch (e: IOException) {
            JSONObject().apply {
                put("action", "logout")
                put("success", false)
                put("error", e.message)
            }
        }
    }

    /**
     * Switch tab in MainNavigationActivity. Must be called on main thread.
     */
    fun switchTab(context: Context, tab: String): JSONObject {
        return try {
            val intent = Intent(context, MainNavigationActivity::class.java).apply {
                putExtra("debug_target_tab", tab)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
            JSONObject().apply {
                put("action", "switch_tab")
                put("success", true)
                put("tab", tab)
            }
        } catch (e: IOException) {
            JSONObject().apply {
                put("action", "switch_tab")
                put("success", false)
                put("tab", tab)
                put("error", e.message)
            }
        }
    }

    /**
     * Open link detail screen by ID. Must be called on main thread.
     */
    fun openLink(context: Context, linkId: Long): JSONObject {
        return try {
            val intent = LinkDetailsActivityV2.createIntent(context, linkId).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            JSONObject().apply {
                put("action", "open_link")
                put("success", true)
                put("link_id", linkId)
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("action", "open_link")
                put("success", false)
                put("link_id", linkId)
                put("error", e.message)
            }
        }
    }

    /**
     * Open entry detail screen by ID. Must be called on main thread.
     */
    fun openEntry(context: Context, entryId: Long): JSONObject {
        return try {
            val intent = EntryActivity.createIntent(
                context = context,
                entryId = entryId,
                commentId = null,
                isRevealed = false,
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            JSONObject().apply {
                put("action", "open_entry")
                put("success", true)
                put("entry_id", entryId)
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("action", "open_entry")
                put("success", false)
                put("entry_id", entryId)
                put("error", e.message)
            }
        }
    }

    fun getVersionName(context: Context): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }

    private fun deleteRecursive(fileOrDirectory: File): Int {
        var count = 0
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.let { files ->
                for (child in files) {
                    count += deleteRecursive(child)
                }
            }
        }
        if (fileOrDirectory.delete()) {
            count++
        }
        return count
    }
}

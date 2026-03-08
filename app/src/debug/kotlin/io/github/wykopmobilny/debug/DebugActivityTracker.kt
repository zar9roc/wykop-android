package io.github.wykopmobilny.debug

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Tracks the currently resumed Activity for debug tools (e.g. DebugStateReceiver).
 * Registered via DebugToolsInitializer in debug builds only.
 */
object DebugActivityTracker : Application.ActivityLifecycleCallbacks {
    var currentActivity: Activity? = null
        private set

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}

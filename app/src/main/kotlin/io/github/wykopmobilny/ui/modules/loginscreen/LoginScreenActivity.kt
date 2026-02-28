package io.github.wykopmobilny.ui.modules.loginscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.databinding.ActivityContainerBinding
import io.github.wykopmobilny.ui.login.android.handleLoginV3Callback
import io.github.wykopmobilny.ui.login.android.loginV3Fragment
import io.github.wykopmobilny.utils.viewBinding

internal class LoginScreenActivity : ThemableActivity() {
    private val binding by viewBinding(ActivityContainerBinding::inflate)
    private var pendingDeepLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Napier.d(tag = TAG) { "onCreate: savedInstanceState=${savedInstanceState != null}, intent.data=${intent?.data}" }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, loginV3Fragment())
                .commit()
        }

        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Process pending deep link if fragment wasn't ready earlier
        pendingDeepLink?.let { deepLink ->
            Napier.i(tag = TAG) { "onResume: Processing pending deep link: $deepLink" }
            val fragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
            if (fragment != null) {
                fragment.handleLoginV3Callback(deepLink)
                pendingDeepLink = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Napier.i(tag = TAG) { "onNewIntent: Received new intent with data: ${intent.data}" }
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.data == null) {
            Napier.d(tag = TAG) { "handleIntent: No data in intent, ignoring" }
            return
        }

        intent.data?.let { uri ->
            Napier.i(tag = TAG) { "handleIntent: Deep link received: $uri" }
            val fragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
            if (fragment != null) {
                Napier.d(tag = TAG) { "handleIntent: Found fragment, passing callback to handleLoginV3Callback" }
                fragment.handleLoginV3Callback(uri.toString())
            } else {
                Napier.w(tag = TAG) { "handleIntent: Fragment not found in container, saving as pending" }
                pendingDeepLink = uri.toString()
            }
        }
    }

    companion object {
        private const val TAG = "LoginScreenActivity"

        fun createIntent(context: Context) = Intent(context, LoginScreenActivity::class.java)
    }
}

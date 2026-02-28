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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, loginV3Fragment())
                .commit()
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            Napier.d(tag = TAG) { "handleIntent: Deep link received: $uri" }
            supportFragmentManager.findFragmentById(binding.fragmentContainer.id)?.handleLoginV3Callback(uri.toString())
        }
    }

    companion object {
        private const val TAG = "LoginScreenActivity"

        fun createIntent(context: Context) = Intent(context, LoginScreenActivity::class.java)
    }
}

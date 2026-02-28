package io.github.wykopmobilny.ui.login.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.ui.login.LoginDependencies
import io.github.wykopmobilny.ui.login.LoginV3
import io.github.wykopmobilny.ui.login.android.databinding.FragmentLoginV3Binding
import io.github.wykopmobilny.utils.bindings.collectErrorDialog
import io.github.wykopmobilny.utils.requireDependency
import io.github.wykopmobilny.utils.viewBinding
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun loginV3Fragment(): Fragment = LoginV3Fragment()

internal class LoginV3Fragment : Fragment(R.layout.fragment_login_v3) {
    private val binding by viewBinding(FragmentLoginV3Binding::bind)
    private lateinit var loginV3: LoginV3

    override fun onAttach(context: Context) {
        loginV3 = context.requireDependency<LoginDependencies>().loginV3()
        super.onAttach(context)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeState()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            Napier.i(tag = TAG) { "loginButton clicked: Starting login flow" }
            loginV3.login()
        }

        binding.copyUrlButton.setOnClickListener {
            val url = binding.connectUrlText.text?.toString() ?: return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("login_url", url))
            Toast.makeText(requireContext(), getString(R.string.login_url_copied), Toast.LENGTH_SHORT).show()
        }

        binding.openBrowserButton.setOnClickListener {
            val url = binding.connectUrlText.text?.toString() ?: return@setOnClickListener
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        binding.submitCallbackButton.setOnClickListener {
            val callbackUrl =
                binding.callbackUrlInput.text
                    ?.toString()
                    ?.trim()
            if (callbackUrl.isNullOrBlank()) {
                binding.callbackUrlInputLayout.error = getString(R.string.login_paste_url)
                return@setOnClickListener
            }
            binding.callbackUrlInputLayout.error = null
            submitCallbackUrl(callbackUrl)
        }

        binding.switchToOAuthButton.setOnClickListener {
            Napier.i(tag = TAG) { "switchToOAuthButton clicked: Switching to OAuth login" }
            val containerId = (requireView().parent as? View)?.id ?: return@setOnClickListener
            parentFragmentManager
                .beginTransaction()
                .replace(containerId, loginFragment())
                .commit()
        }
    }

    private fun submitCallbackUrl(url: String) {
        Napier.i(tag = TAG) { "submitCallbackUrl: URL=$url" }
        viewLifecycleOwner.lifecycleScope.launch {
            val state = loginV3().stateIn(viewLifecycleOwner.lifecycleScope).value
            val isCallback = state.isCallbackUrl(url)
            Napier.d(tag = TAG) { "submitCallbackUrl: isCallbackUrl=$isCallback" }
            if (isCallback) {
                Napier.i(tag = TAG) { "submitCallbackUrl: Valid callback URL, parsing credentials" }
                state.parseUrlAction(url)
            } else {
                Napier.w(tag = TAG) { "submitCallbackUrl: Invalid callback URL" }
                binding.callbackUrlInputLayout.error = getString(R.string.login_invalid_url)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val sharedFlow = loginV3().stateIn(this)

                launch {
                    sharedFlow
                        .map { it.connectUrl }
                        .distinctUntilChanged()
                        .collect { connectUrl ->
                            Napier.i(tag = TAG) { "connectUrl changed: $connectUrl" }
                            val hasUrl = connectUrl != null
                            binding.urlCard.isVisible = hasUrl
                            binding.callbackCard.isVisible = hasUrl
                            if (hasUrl) {
                                binding.connectUrlText.text = connectUrl
                            }
                        }
                }

                launch {
                    sharedFlow
                        .map { it.isLoading }
                        .distinctUntilChanged()
                        .collect { isLoading ->
                            Napier.i(tag = TAG) { "isLoading changed: $isLoading" }
                            binding.fullScreenProgress.isVisible = isLoading
                            binding.loginButton.isEnabled = !isLoading
                        }
                }

                launch {
                    sharedFlow
                        .map { it.errorDialog }
                        .collectErrorDialog(requireContext())
                }

                launch {
                    sharedFlow
                        .map { it.isLoggedIn }
                        .distinctUntilChanged()
                        .collect { isLoggedIn ->
                            Napier.i(tag = TAG) { "isLoggedIn changed: $isLoggedIn" }
                            if (isLoggedIn) {
                                Napier.i(tag = TAG) { "Login successful, finishing activity" }
                                Toast
                                    .makeText(
                                        requireContext(),
                                        getString(R.string.login_success),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                requireActivity().finish()
                            }
                        }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LoginV3Fragment"
    }
}

package io.github.wykopmobilny.ui.login.android

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

        setupWebView()
        setupClickListeners()
        observeState()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        Napier.d(tag = TAG) { "setupWebView: Initializing WebView settings" }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }
        binding.webView.settings.javaScriptEnabled = true
        Napier.d(tag = TAG) { "setupWebView: WebView configured successfully" }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            Napier.i(tag = TAG) { "loginButton clicked: Starting login flow" }
            loginV3.login()
        }

        binding.switchToOAuthButton.setOnClickListener {
            Napier.i(tag = TAG) { "switchToOAuthButton clicked: Switching to OAuth login" }
            // Switch back to OAuth login
            val containerId = (requireView().parent as? View)?.id ?: return@setOnClickListener
            parentFragmentManager
                .beginTransaction()
                .replace(containerId, loginFragment())
                .commit()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val sharedFlow = loginV3().stateIn(this)

                binding.webView.webViewClient =
                    object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: android.graphics.Bitmap?,
                        ) {
                            Napier.d(tag = TAG) { "onPageStarted: $url" }
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            Napier.d(tag = TAG) { "onPageFinished: $url" }
                            super.onPageFinished(view, url)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            Napier.e(tag = TAG) {
                                "onReceivedError: url=${request?.url}, errorCode=${error?.errorCode}, description=${error?.description}"
                            }
                            super.onReceivedError(view, request, error)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val url = request.url.toString()
                            Napier.d(tag = TAG) { "shouldOverrideUrlLoading: URL=$url" }
                            val state = sharedFlow.value
                            val isCallback = state.isCallbackUrl(url)
                            Napier.d(tag = TAG) { "shouldOverrideUrlLoading: isCallbackUrl=$isCallback" }
                            if (isCallback) {
                                Napier.i(tag = TAG) { "shouldOverrideUrlLoading: Callback URL detected, parsing credentials" }
                                state.parseUrlAction(url)
                                Napier.i(tag = TAG) { "shouldOverrideUrlLoading: parseUrlAction completed" }
                                return true
                            }
                            return false
                        }
                    }

                launch {
                    sharedFlow
                        .map { it.connectUrl }
                        .distinctUntilChanged()
                        .collect { connectUrl ->
                            Napier.i(tag = TAG) { "connectUrl changed: $connectUrl" }
                            val showWebView = connectUrl != null
                            binding.webView.isVisible = showWebView
                            binding.loginCard.isVisible = !showWebView
                            if (connectUrl != null) {
                                Napier.i(tag = TAG) { "Loading connect URL in WebView" }
                                Napier.d(tag = TAG) { "Removing all cookies before loading URL" }
                                removeAllCookiesAndWait()
                                Napier.d(tag = TAG) { "Cookies removed, loading URL" }
                                binding.webView.loadUrl(connectUrl)
                            } else {
                                Napier.d(tag = TAG) { "WebView hidden, showing login card" }
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
                                Toast.makeText(requireContext(), "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                requireActivity().finish()
                            }
                        }
                }
            }
        }
    }

    /**
     * Suspends until all cookies are removed from CookieManager.
     * This ensures that the WebView loads the login page without any existing session cookies,
     * preventing automatic redirects before the user can interact with the GDPR overlay.
     */
    private suspend fun removeAllCookiesAndWait() =
        suspendCancellableCoroutine { continuation ->
            CookieManager.getInstance().removeAllCookies { success ->
                Napier.d(tag = TAG) { "removeAllCookiesAndWait: success=$success" }
                continuation.resume(Unit)
            }
        }

    companion object {
        private const val TAG = "LoginV3Fragment"
    }
}

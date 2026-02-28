package io.github.wykopmobilny.ui.login.android

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

        setupWebView()
        setupClickListeners()
        observeState()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.webView, true)
        }
        binding.webView.settings.javaScriptEnabled = true
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            loginV3.login()
        }

        binding.switchToOAuthButton.setOnClickListener {
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
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            sharedFlow.value.parseUrlAction(request.url.toString())
                            return super.shouldOverrideUrlLoading(view, request)
                        }
                    }

                launch {
                    sharedFlow
                        .map { it.connectUrl }
                        .distinctUntilChanged()
                        .collect { connectUrl ->
                            val showWebView = connectUrl != null
                            binding.webView.isVisible = showWebView
                            binding.loginCard.isVisible = !showWebView
                            if (connectUrl != null) {
                                binding.webView.loadUrl(connectUrl)
                            }
                        }
                }

                launch {
                    sharedFlow
                        .map { it.isLoading }
                        .distinctUntilChanged()
                        .collect { isLoading ->
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
                            if (isLoggedIn) {
                                Toast.makeText(requireContext(), "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                requireActivity().finish()
                            }
                        }
                }
            }
        }
    }
}

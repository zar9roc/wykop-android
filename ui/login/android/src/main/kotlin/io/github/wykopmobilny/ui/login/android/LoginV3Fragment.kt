package io.github.wykopmobilny.ui.login.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import io.github.wykopmobilny.ui.login.LoginV3Ui
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

    // Chroni przed wielokrotnym parsowaniem tego samego callbacku -
    // redirect potrafi przejsc przez kilka callbackow WebViewClient.
    private var callbackHandled = false

    // Ostatni stan Ui - callbacki WebViewClient sa synchroniczne,
    // wiec nie moga czekac na Flow.
    private var latestUi: LoginV3Ui? = null

    // Automatyczny start flow po wejsciu na ekran - raz na instancje fragmentu.
    private var autoLoginTriggered = false

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
        observeState()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(binding.loginWebView, true)
        }
        binding.loginWebView.settings.apply {
            // Strona logowania wykop.pl to aplikacja JS - bez domStorage
            // renderuje sie pusta albo przycisk logowania nie dziala.
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        binding.loginWebView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val url = request.url.toString()
                    Napier.d(tag = TAG) { "WebView shouldOverrideUrlLoading: ${url.take(URL_LOG_LENGTH)}" }
                    return handleUrl(url)
                }

                // shouldOverrideUrlLoading NIE jest wolane dla redirectow serwera (302)
                // ani zmian location przez JS - finalny callback z tokenami przychodzi
                // wlasnie tak, wiec sprawdzamy URL takze tutaj.
                override fun onPageStarted(
                    view: WebView,
                    url: String,
                    favicon: Bitmap?,
                ) {
                    Napier.d(tag = TAG) { "WebView onPageStarted: ${url.take(URL_LOG_LENGTH)}" }
                    if (handleUrl(url)) {
                        view.stopLoading()
                    }
                    super.onPageStarted(view, url, favicon)
                }

                override fun doUpdateVisitedHistory(
                    view: WebView,
                    url: String,
                    isReload: Boolean,
                ) {
                    Napier.d(tag = TAG) { "WebView doUpdateVisitedHistory: ${url.take(URL_LOG_LENGTH)}" }
                    if (handleUrl(url)) {
                        view.stopLoading()
                    }
                    super.doUpdateVisitedHistory(view, url, isReload)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        Napier.e(tag = TAG) { "WebView onReceivedError: ${error.errorCode} ${error.description}" }
                        binding.loginWebView.isVisible = false
                    }
                    super.onReceivedError(view, request, error)
                }
            }
    }

    /**
     * Zwraca true gdy URL to callback z tokenami - wtedy nawigacja jest blokowana,
     * a token/rtoken z query params trafiaja do [LoginV3Ui.parseUrlAction].
     */
    private fun handleUrl(url: String): Boolean {
        if (callbackHandled) return true
        val state = latestUi ?: return false
        if (!state.isCallbackUrl(url)) return false
        callbackHandled = true
        Napier.i(tag = TAG) { "WebView captured callback URL, parsing credentials" }
        binding.loginWebView.isVisible = false
        binding.loginWebView.stopLoading()
        state.parseUrlAction(url)
        return true
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val sharedFlow = loginV3().stateIn(this)

                launch { sharedFlow.collect { latestUi = it } }

                // "Zaloguj sie" prowadzi bezposrednio do WebView - startujemy flow
                // automatycznie zamiast czekac na klikniecie przycisku z kroku 1.
                val initial = sharedFlow.value
                if (!autoLoginTriggered && initial.connectUrl == null && !initial.isLoading) {
                    autoLoginTriggered = true
                    Napier.i(tag = TAG) { "Auto-starting login flow (direct WebView)" }
                    loginV3.login()
                }

                launch {
                    sharedFlow
                        .map { it.connectUrl }
                        .distinctUntilChanged()
                        .collect { connectUrl ->
                            Napier.i(tag = TAG) { "connectUrl changed: $connectUrl" }
                            if (connectUrl != null) {
                                callbackHandled = false
                                binding.loginWebView.isVisible = true
                                Napier.i(tag = TAG) { "Loading connectUrl in WebView" }
                                binding.loginWebView.loadUrl(connectUrl)
                            } else {
                                binding.loginWebView.isVisible = false
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

        // Nie logujemy pelnych URLi - callback zawiera tokeny.
        private const val URL_LOG_LENGTH = 60
    }
}

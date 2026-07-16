package io.github.wykopmobilny.ui.modules.report

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.ThemableActivity

/**
 * Zgłaszanie treści: API v3 nie ma endpointu zgłoszeń (v2 dawało violation_url
 * z serwerowym hashem), więc otwieramy stronę treści na wykop.pl we WebView.
 * WebView współdzieli CookieManager z ekranem logowania - użytkownik ma sesję
 * i może dokończyć zgłoszenie przez interfejs strony.
 */
internal class ReportWebViewActivity : ThemableActivity() {
    companion object {
        private const val EXTRA_URL = "EXTRA_URL"

        fun createIntent(
            context: Context,
            url: String,
        ) = Intent(context, ReportWebViewActivity::class.java).apply {
            putExtra(EXTRA_URL, url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_webview)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = getString(R.string.report)
            setDisplayHomeAsUpEnabled(true)
        }

        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val webView = findViewById<WebView>(R.id.webView)
        CookieManager.getInstance().setAcceptCookie(true)
        webView.settings.apply {
            // wykop.pl to aplikacja JS - bez domStorage strona się nie renderuje
            // (te same wymagania co WebView logowania).
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.webViewClient = WebViewClient()

        // targetSdk 36: "wstecz" wyłącznie przez dispatcher (onBackPressed martwy).
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                finish()
            }
        }

        webView.loadUrl(url)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

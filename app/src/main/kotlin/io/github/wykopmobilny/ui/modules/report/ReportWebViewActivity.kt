package io.github.wykopmobilny.ui.modules.report

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.reports.CreateProfileReportRequestV3
import io.github.wykopmobilny.api.requests.v3.reports.CreateReportRequestV3
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.ui.dialogs.showExceptionDialog
import kotlinx.coroutines.launch

/**
 * Zgłaszanie treści. Adres formularza (z serwerowym hashem) wydaje dopiero
 * POST /v3/reports/reports - tak samo robi frontend wykop.pl, który przechodzi
 * pod zwrócone `data.url`. Sam formularz jest webowy, więc ładujemy go we
 * WebView współdzielącym CookieManager z ekranem logowania (użytkownik ma sesję).
 */
internal class ReportWebViewActivity : ThemableActivity() {
    companion object {
        private const val EXTRA_TYPE = "EXTRA_TYPE"
        private const val EXTRA_ID = "EXTRA_ID"
        private const val EXTRA_PARENT_ID = "EXTRA_PARENT_ID"

        fun createIntent(
            context: Context,
            type: ReportType,
            id: String,
            parentId: Long? = null,
        ) = Intent(context, ReportWebViewActivity::class.java).apply {
            putExtra(EXTRA_TYPE, type.apiValue)
            putExtra(EXTRA_ID, id)
            parentId?.let { putExtra(EXTRA_PARENT_ID, it) }
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

        val type = intent.getStringExtra(EXTRA_TYPE) ?: return finish()
        val id = intent.getStringExtra(EXTRA_ID)?.takeIf { it.isNotBlank() } ?: return finish()
        val parentId = intent.getLongExtra(EXTRA_PARENT_ID, -1L).takeIf { it > 0 }

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

        loadReportForm(webView, type, id, parentId)
    }

    private fun loadReportForm(
        webView: WebView,
        type: String,
        id: String,
        parentId: Long?,
    ) = lifecycleScope.launch {
        val reportsApi = (application as WykopApp).wykopApi.reportsV3RetrofitApi()
        val url =
            runCatching {
                // Profil nie ma id liczbowego - identyfikuje go username, stad osobne body.
                if (type == ReportType.Profile.apiValue) {
                    reportsApi.createProfileReport(WykopApiRequestV3(CreateProfileReportRequestV3(username = id)))
                } else {
                    reportsApi.createReport(
                        WykopApiRequestV3(
                            CreateReportRequestV3(type = type, id = id.toLong(), parentId = parentId),
                        ),
                    )
                }.data?.url
            }.onFailure { failure ->
                Napier.w("Nie udalo sie utworzyc zgloszenia", failure)
                showExceptionDialog(failure)
            }.getOrNull()

        if (url == null) {
            finish()
        } else {
            webView.loadUrl(url)
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

/**
 * Rodzaje zgłaszanych treści wspierane przez /v3/reports/reports. Komentarze
 * wymagają dodatkowo `parent_id` - id wpisu/znaleziska, pod którym stoją.
 */
enum class ReportType(
    val apiValue: String,
) {
    Entry("entry"),
    EntryComment("entry_comment"),
    Link("link"),
    LinkComment("link_comment"),

    /** Powiązane znalezisko: id = powiązania, parent_id = znaleziska. */
    LinkRelated("link_related"),

    /** Profil: id to `username` (API v3 nie ma numerycznego id użytkownika). */
    Profile("profile"),
}

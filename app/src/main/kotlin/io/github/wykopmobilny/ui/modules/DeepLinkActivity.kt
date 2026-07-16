package io.github.wykopmobilny.ui.modules

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.TaskStackBuilder
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler

class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.dataString ?: return finish()
        val activityToOpen = WykopLinkHandler.getLinkIntent(url, this)
        if (activityToOpen != null) {
            // Syntetyczny back-stack: pod celem ląduje główny ekran (dla wpisu -
            // mikroblog, dla znaleziska - lista linków), żeby "wstecz" prowadziło
            // do aplikacji zamiast ją zamykać (wcześniej cel był rootem taska).
            TaskStackBuilder
                .create(this)
                .addNextIntent(MainNavigationActivity.getIntent(this, url.mainScreenTarget()))
                .addNextIntent(activityToOpen)
                .startActivities()
        } else {
            Napier.e("Invalid deeplink for url=$url")
            startActivity(MainNavigationActivity.getIntent(this))
        }
        overridePendingTransition(0, 0)
        finish()
    }

    // Mapowanie typu zasobu na zakładkę głównego ekranu (klucze jak w
    // MainNavigationActivity.navigationTargets).
    private fun String.mainScreenTarget(): String? =
        when (substringAfter("wykop.pl/", missingDelimiterValue = "").substringBefore("/")) {
            "wpis" -> "hot"
            "link" -> "promoted"
            else -> null
        }
}

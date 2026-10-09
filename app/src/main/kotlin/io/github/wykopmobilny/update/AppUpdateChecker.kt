package io.github.wykopmobilny.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.BuildConfig
import io.github.wykopmobilny.ui.settings.AppUpdates
import io.github.wykopmobilny.ui.settings.UpdateCheckFrequency
import io.github.wykopmobilny.ui.settings.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Wydania forka na GitHubie (tagi zar-vX.Y.Z, workflow release_fork.yml):
 * reczne sprawdzenie z ustawien i harmonogram [UpdateCheckWorker].
 *
 * /releases/latest pomija drafty i prerelease'y, wiec nowa wersja jest widoczna
 * dopiero po opublikowaniu wydania.
 */
class AppUpdateChecker(
    context: Context,
) : AppUpdates {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override val installedVersion: String = BuildConfig.VERSION_NAME

    override val isCheckFrequencyChosen: Boolean
        get() = prefs.contains(PREF_FREQUENCY)

    override var checkFrequency: UpdateCheckFrequency
        get() =
            prefs
                .getString(PREF_FREQUENCY, null)
                ?.let { stored -> UpdateCheckFrequency.entries.firstOrNull { it.name == stored } }
                ?: UpdateCheckFrequency.Daily
        set(value) {
            prefs.edit().putString(PREF_FREQUENCY, value.name).apply()
            schedule()
        }

    override suspend fun checkNow(): UpdateCheckResult =
        runCatching { fetchLatestRelease() }
            .fold(
                onSuccess = { release ->
                    if (release != null && isNewerRelease(release.tag, installedVersion)) {
                        UpdateCheckResult.Available(version = release.version, downloadUrl = release.url)
                    } else {
                        UpdateCheckResult.UpToDate
                    }
                },
                onFailure = {
                    Napier.w("Update check failed: ${it.message}")
                    UpdateCheckResult.Failed
                },
            )

    /**
     * Harmonogram wg [checkFrequency]. Bez zgody z pytania przy pierwszym uruchomieniu
     * nic nie sprawdzamy w tle. Buildy debug maja wersje -SNAPSHOT, wiec ich nie dotyczy.
     */
    fun schedule() {
        if (BuildConfig.DEBUG || !isCheckFrequencyChosen) return
        runCatching {
            val workManager = WorkManager.getInstance(context)
            val days =
                when (checkFrequency) {
                    UpdateCheckFrequency.Daily -> 1L
                    UpdateCheckFrequency.Weekly -> 7L
                    UpdateCheckFrequency.Never -> {
                        workManager.cancelUniqueWork(WORK_NAME)
                        return
                    }
                }
            val request =
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(days, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()
            // UPDATE: zmiana okresu w ustawieniach dziala bez kasowania postepu biezacego cyklu.
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }.onFailure { Napier.e("Failed to schedule update check", it) }
    }

    /** O danej wersji powiadamiamy w tle tylko raz. */
    fun wasNotified(tag: String) = prefs.getString(PREF_NOTIFIED_TAG, null) == tag

    fun markNotified(tag: String) = prefs.edit().putString(PREF_NOTIFIED_TAG, tag).apply()

    suspend fun fetchLatestRelease(): Release? =
        withContext(Dispatchers.IO) {
            val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                // 404 = repozytorium nie ma jeszcze zadnego opublikowanego wydania.
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                Release(tag = json.getString("tag_name"), url = json.getString("html_url"))
            } finally {
                connection.disconnect()
            }
        }

    data class Release(
        val tag: String,
        val url: String,
    ) {
        val version: String
            get() = versionOf(tag)?.joinToString(".") ?: tag
    }

    companion object {
        private const val WORK_NAME = "update_check"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/zar9roc/wykop-android/releases/latest"
        private const val TIMEOUT_MS = 15_000
        private const val PREFS = "update_check"
        private const val PREF_NOTIFIED_TAG = "notified_tag"
        private const val PREF_FREQUENCY = "frequency"

        private val VERSION_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")

        internal fun versionOf(text: String): List<Int>? = VERSION_REGEX.find(text)?.groupValues?.drop(1)?.map(String::toInt)

        /** "zar-v1.5.2" vs "1.5.1" -> true. Sufiksy (-SNAPSHOT, -debug, licznik) sa ignorowane. */
        internal fun isNewerRelease(
            tag: String,
            installedVersion: String,
        ): Boolean {
            val latest = versionOf(tag) ?: return false
            val installed = versionOf(installedVersion) ?: return false
            for (i in latest.indices) {
                if (latest[i] != installed[i]) return latest[i] > installed[i]
            }
            return false
        }
    }
}

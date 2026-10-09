package io.github.wykopmobilny.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.BuildConfig
import io.github.wykopmobilny.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Raz dziennie sprawdza najnowsze wydanie forka na GitHubie (tagi zar-vX.Y.Z,
 * workflow release_fork.yml) i jesli jest nowsze od zainstalowanej wersji -
 * pokazuje powiadomienie prowadzace do strony wydania.
 *
 * /releases/latest pomija drafty i prerelease'y, wiec powiadomienie pojawia sie
 * dopiero po opublikowaniu wydania. O danej wersji powiadamiamy tylko raz.
 *
 * Tworzony domyslnym konstruktorem WorkManagera - InjectingFactory zwraca null
 * dla nieznanych klas i WorkManager robi fallback na refleksje.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val release =
            runCatching { fetchLatestRelease() }
                .getOrElse { failure ->
                    Napier.w("Update check failed: ${failure.message}")
                    return Result.success()
                } ?: return Result.success()

        if (!isNewerRelease(release.tag, BuildConfig.VERSION_NAME)) return Result.success()

        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(PREF_NOTIFIED_TAG, null) == release.tag) return Result.success()

        if (showNotification(release)) {
            prefs.edit().putString(PREF_NOTIFIED_TAG, release.tag).apply()
        }
        return Result.success()
    }

    private suspend fun fetchLatestRelease(): Release? =
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

    /** @return false, gdy uzytkownik nie dal zgody na powiadomienia - wtedy sprobujemy jutro. */
    private fun showNotification(release: Release): Boolean {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService<NotificationManager>()?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.update_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val version = versionOf(release.tag) ?: release.tag
        val openRelease =
            PendingIntent.getActivity(
                context,
                0,
                Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(io.github.wykopmobilny.ui.notifications.android.R.drawable.ic_wykopmobilny)
                .setContentTitle(context.getString(R.string.update_available_title))
                .setContentText(context.getString(R.string.update_available_content, version))
                .setContentIntent(openRelease)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    private data class Release(
        val tag: String,
        val url: String,
    )

    companion object {
        private const val WORK_NAME = "update_check"
        private const val LATEST_RELEASE_URL = "https://api.github.com/repos/zar9roc/wykop-android/releases/latest"
        private const val CHANNEL_ID = "io.github.wykopmobilny:update"
        private const val NOTIFICATION_ID = 127
        private const val TIMEOUT_MS = 15_000
        private const val PREFS = "update_check"
        private const val PREF_NOTIFIED_TAG = "notified_tag"

        private val VERSION_REGEX = Regex("""(\d+)\.(\d+)\.(\d+)""")

        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()
            runCatching {
                WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            }.onFailure { Napier.e("Failed to schedule update check", it) }
        }

        internal fun versionOf(text: String): List<Int>? =
            VERSION_REGEX.find(text)?.groupValues?.drop(1)?.map(String::toInt)

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

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.BuildConfig
import io.github.wykopmobilny.R

/**
 * Okresowe (wg ustawienia "Czestotliwosc sprawdzania") sprawdzenie nowego wydania
 * forka - jesli jest nowsze od zainstalowanej wersji, powiadomienie prowadzace do
 * strony wydania. O danej wersji powiadamiamy tylko raz.
 *
 * Tworzony domyslnym konstruktorem WorkManagera - InjectingFactory zwraca null
 * dla nieznanych klas i WorkManager robi fallback na refleksje.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val checker = AppUpdateChecker(applicationContext)
        val release =
            runCatching { checker.fetchLatestRelease() }
                .getOrElse { failure ->
                    Napier.w("Update check failed: ${failure.message}")
                    return Result.success()
                } ?: return Result.success()

        if (!AppUpdateChecker.isNewerRelease(release.tag, BuildConfig.VERSION_NAME)) return Result.success()
        if (checker.wasNotified(release.tag)) return Result.success()

        if (showNotification(release)) checker.markNotified(release.tag)
        return Result.success()
    }

    /** @return false, gdy uzytkownik nie dal zgody na powiadomienia - wtedy sprobujemy przy nastepnym sprawdzeniu. */
    private fun showNotification(release: AppUpdateChecker.Release): Boolean {
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
                .setContentText(context.getString(R.string.update_available_content, release.version))
                .setContentIntent(openRelease)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }

    companion object {
        private const val CHANNEL_ID = "io.github.wykopmobilny:update"
        private const val NOTIFICATION_ID = 127
    }
}

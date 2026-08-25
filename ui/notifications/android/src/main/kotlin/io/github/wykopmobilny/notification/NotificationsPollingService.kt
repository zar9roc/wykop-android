package io.github.wykopmobilny.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.ui.notifications.android.R
import io.github.wykopmobilny.utils.requireDependency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Opcjonalne czeste sprawdzanie powiadomien (co ~1 min) - foreground service typu specialUse.
 *
 * WorkManager nie schodzi ponizej 15 min, a API v3 nie ma pusha - serwis pierwszoplanowy
 * to jedyny sposob na czestszy polling. Kazdy cykl to jeden tani GET /notifications/status;
 * pelne listy pilnych kanalow (PM / do mnie / obserwowane dyskusje) pobierane tylko przy
 * niezerowych licznikach. Stale powiadomienie serwisu idzie na osobny kanal IMPORTANCE_MIN -
 * uzytkownik moze go wyciszyc/ukryc w ustawieniach systemowych, serwis dziala dalej.
 */
class NotificationsPollingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification())
        if (!started) {
            started = true
            val dependencies = applicationContext.requireDependency<NotificationDependencies>()

            // Interwal sterowany ustawieniem "Czestotliwosc sprawdzania" (< 15 min);
            // null = tryb wylaczony -> serwis konczy prace. collectLatest restartuje
            // petle przy zmianie okresu.
            scope.launch {
                dependencies.frequentPollingInterval().invoke().collectLatest { interval ->
                    if (interval == null) {
                        Napier.i("Frequent polling disabled - stopping service")
                        stopSelf()
                    } else {
                        while (isActive) {
                            runCatching { dependencies.refreshNotifications().invoke(statusFirst = true) }
                                .onFailure { Napier.w("Frequent poll failed", it) }
                            delay(interval)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildServiceNotification(): Notification {
        val manager = getSystemService<NotificationManager>().let(::checkNotNull)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    getString(R.string.channel_name_polling_service),
                    // MIN: bez dzwieku, zwinieta pozycja; kanal mozna wylaczyc systemowo.
                    NotificationManager.IMPORTANCE_MIN,
                ).apply { setShowBadge(false) },
            )
        }
        return NotificationCompat
            .Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wykopmobilny)
            .setContentTitle(getString(R.string.polling_service_title))
            .setContentText(getString(R.string.polling_service_content))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .build()
    }

    companion object {
        private const val SERVICE_NOTIFICATION_ID = 126
        private const val SERVICE_CHANNEL_ID = "io.github.wykopmobilny:notifications:polling_service"

        // Start bezpieczny z dowolnego miejsca: od Androida 12 start FGS z tla rzuca -
        // lykamy blad (serwis wstanie przy najblizszym starcie z foregroundu).
        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, NotificationsPollingService::class.java),
                )
            }.onFailure { Napier.w("Unable to start polling service", it) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationsPollingService::class.java))
        }
    }
}

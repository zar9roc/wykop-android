package io.github.wykopmobilny.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
                        // Liczba kolejnych nieudanych cykli - steruje backoffem, zeby przy
                        // dluzszej awarii sieci nie probowac co minute (kazda nieudana proba
                        // to nawet 3 x connectTimeout na trzech adresach wykop.pl).
                        var failures = 0
                        while (isActive) {
                            if (!hasInternet()) {
                                // Radio spi albo brak trasy - socket i tak skonczy sie
                                // timeoutem, a log zapelni sie stack trace'ami.
                                Napier.d("Frequent poll skipped - brak polaczenia")
                                delay(minOf(interval, OFFLINE_RECHECK_DELAY))
                                continue
                            }
                            runCatching { dependencies.refreshNotifications().invoke(statusFirst = true) }
                                .onSuccess { failures = 0 }
                                .onFailure { failure ->
                                    failures++
                                    if (failure is IOException) {
                                        // Blad sieci jest oczekiwany - sam komunikat wystarczy,
                                        // stack trace tylko zasmieca plik logu.
                                        Napier.w("Frequent poll failed (siec, proba $failures): ${failure.message}")
                                    } else {
                                        Napier.w("Frequent poll failed", failure)
                                    }
                                }
                            delay(nextDelay(interval, failures))
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    /**
     * Czy urzadzenie ma zwalidowane polaczenie z internetem. Bez tego cykl
     * pollingu to tylko palenie radia na timeoutach.
     */
    private fun hasInternet(): Boolean {
        val manager = getSystemService<ConnectivityManager>() ?: return true
        val capabilities = manager.activeNetwork?.let(manager::getNetworkCapabilities) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Po bledzie odstep rosnie wykladniczo (x2, x4, x8...) do [MAX_BACKOFF]. */
    private fun nextDelay(
        interval: Duration,
        failures: Int,
    ): Duration {
        if (failures == 0) return interval
        val multiplier = 1 shl minOf(failures - 1, MAX_BACKOFF_SHIFT)
        return minOf(interval * multiplier, MAX_BACKOFF)
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

        // Jak czesto sprawdzac, czy siec wrocila, gdy jej nie ma.
        private val OFFLINE_RECHECK_DELAY = 1.minutes

        // Sufit backoffu po serii bledow sieci.
        private val MAX_BACKOFF = 30.minutes
        private const val MAX_BACKOFF_SHIFT = 5

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

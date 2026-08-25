package io.github.wykopmobilny.initializers

import android.content.Context
import android.os.Build
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.wykopmobilny.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zapisuje logi i awarie do pliku na urzadzeniu zamiast wysylac je do uslugi
 * webowej. Pliki lezą w katalogu app-specific external storage
 * (Android/data/<pkg>/files/crashlogs), widocznym przez menedzer plikow / USB
 * i kasowanym razem z aplikacja - bez potrzeby uprawnien runtime.
 *
 * Instalowany jako Napier Antilog (loguje WARNING+ oraz kazdy wyjatek).
 * Dodatkowo [installAsUncaughtExceptionHandler] przechwytuje twarde awarie
 * (nielapane wyjatki), ktorych samo logowanie by nie zlapalo.
 */
internal class FileLogAntilog(context: Context) : Antilog() {

    private val logDir: File =
        (context.applicationContext.getExternalFilesDir(null) ?: context.applicationContext.filesDir)
            .resolve("crashlogs")
            .apply { mkdirs() }

    private val logFile: File
        get() = logDir.resolve("log.txt")

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    // Nagłówek diagnostyczny doklejany przed każdym stack trace'em - przy analizie
    // wyeksportowanych logów od razu wiadomo, która wersja aplikacji je wyprodukowała
    // (plik przeżywa aktualizacje, więc wpisy z różnych wersji mieszają się w jednym logu).
    private val diagnosticHeader =
        "--- app ${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_COUNTER} " +
            "(${BuildConfig.VERSION_CODE}, ${BuildConfig.BUILD_TYPE}) | " +
            "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) | " +
            "${Build.MANUFACTURER} ${Build.MODEL} ---"

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?,
    ) {
        // Zapisujemy tylko istotne wpisy - ostrzezenia/bledy oraz wszystko z wyjatkiem.
        if (priority < LogLevel.WARNING && throwable == null) return
        write(format(priority.name, tag, message, throwable))
    }

    private fun logFatal(thread: Thread, throwable: Throwable) {
        write(format("FATAL", thread.name, "Nieprzechwycony wyjatek", throwable))
    }

    /**
     * Podpina sie pod globalny handler nielapanych wyjatkow, zapisuje awarie do
     * pliku, a nastepnie deleguje do dotychczasowego handlera (np. domyslnego
     * systemowego, ktory ubija proces).
     */
    fun installAsUncaughtExceptionHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { logFatal(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun format(
        level: String,
        tag: String?,
        message: String?,
        throwable: Throwable?,
    ): String =
        buildString {
            append(timestampFormat.format(Date()))
            append(' ').append(level)
            if (!tag.isNullOrBlank()) append(" [").append(tag).append(']')
            if (!message.isNullOrBlank()) append(' ').append(message)
            if (throwable != null) {
                append('\n').append(diagnosticHeader)
                append('\n').append(throwable.stackTraceToString().trimEnd())
            }
            append('\n')
        }

    @Synchronized
    private fun write(entry: String) {
        runCatching {
            rotateIfNeeded()
            logFile.appendText(entry)
        }
    }

    private fun rotateIfNeeded() {
        val file = logFile
        if (file.exists() && file.length() > MAX_FILE_BYTES) {
            val backup = logDir.resolve("log.1.txt")
            if (backup.exists()) backup.delete()
            file.renameTo(backup)
        }
    }

    private companion object {
        const val MAX_FILE_BYTES = 512L * 1024 // 512 KB, trzymamy jeden plik zapasowy
    }
}

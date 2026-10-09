package io.github.wykopmobilny.ui.settings

/**
 * Sprawdzanie nowych wydan aplikacji na GitHubie. Implementuje je Application -
 * ekran ustawien siega po nie przez applicationContext (modul ustawien nie widzi
 * klas aplikacji).
 */
interface AppUpdates {
    val installedVersion: String

    /** false do czasu odpowiedzi na pytanie przy pierwszym uruchomieniu (albo wyboru w ustawieniach). */
    val isCheckFrequencyChosen: Boolean

    /** Zmiana od razu przestawia harmonogram sprawdzania w tle. */
    var checkFrequency: UpdateCheckFrequency

    suspend fun checkNow(): UpdateCheckResult
}

enum class UpdateCheckFrequency {
    Daily,
    Weekly,
    Never,
}

sealed interface UpdateCheckResult {
    /** [downloadUrl] - strona wydania na GitHubie (z plikiem APK do pobrania). */
    data class Available(
        val version: String,
        val downloadUrl: String,
    ) : UpdateCheckResult

    data object UpToDate : UpdateCheckResult

    data object Failed : UpdateCheckResult
}

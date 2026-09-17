package io.github.wykopmobilny.ui.settings

import io.github.wykopmobilny.ui.base.Query

interface GetGeneralPreferences : Query<GeneralPreferencesUi>

data class GeneralPreferencesUi(
    val notifications: NotificationsUi,
    val filtering: FilteringUi,
    val advanced: AdvancedUi,
) {
    // Ustawienia zaawansowane: wlasny klucz API v3 (puste pola = klucz wbudowany).
    class AdvancedUi(
        val apiKey: ApiKeyUi,
    ) {
        /**
         * Wlasny klucz API v3 - jedna pozycja otwierajaca okienko z polami klucz+sekret.
         * [testCredentials] sprawdza pare na serwerze (POST /v3/auth) bez zapisywania;
         * [saveAction] z null/null wraca do klucza wbudowanego.
         *
         * Sekret jest write-only: raz zapisany nie wraca do UI (brak pola z aktualna
         * wartoscia) - kazda zmiana wymaga ponownego wpisania go w calosci.
         */
        class ApiKeyUi(
            val currentKey: String?,
            val testCredentials: suspend (key: String, secret: String) -> Boolean,
            val saveAction: (key: String?, secret: String?) -> Unit,
        )
    }

    data class NotificationsUi(
        val notificationsEnabled: Setting,
        val notificationRefreshPeriod: ListSetting<RefreshPeriodUi>,
        val exitConfirmation: Setting,
    ) {
        enum class RefreshPeriodUi {
            // Okresy < 15 min: foreground service (patrz dialog wyjasniajacy przy wyborze).
            OneMinute,
            FiveMinutes,
            FifteenMinutes,
            ThirtyMinutes,
            OneHour,
            TwoHours,
            FourHours,
            EightHours,
        }
    }
}

class FilteringUi(
    val showPlus18Content: Setting,
    val hideNsfwContent: Setting,
    val hideNewUserContent: Setting,
    val hideContentWithNoTags: Setting,
    val hideBlacklistedContent: Setting,
    val manageBlackList: (() -> Unit)?,
    val useEmbeddedBrowser: Setting,
    val clearSearchHistory: () -> Unit,
)

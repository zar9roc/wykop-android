package io.github.wykopmobilny.ui.settings

import io.github.wykopmobilny.ui.base.Query

interface GetGeneralPreferences : Query<GeneralPreferencesUi>

data class GeneralPreferencesUi(
    val notifications: NotificationsUi,
    val filtering: FilteringUi,
) {
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

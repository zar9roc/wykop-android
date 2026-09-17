package io.github.wykopmobilny.domain.settings.prefs

import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.domain.settings.UserSettings
import io.github.wykopmobilny.domain.settings.get
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class GetMikroblogPreferences
    @Inject
    constructor(
        private val appStorage: AppStorage,
    ) {
        operator fun invoke() =
            combine(
                appStorage.get(UserSettings.mikroblogScreen),
                appStorage.get(UserSettings.cutLongEntries),
                appStorage.get(UserSettings.openSpoilersInDialog),
                appStorage.get(UserSettings.showTopComments),
            ) { defaultScreen, cutLongEntries, openSpoilersInDialog, showTopComments ->
                MikroblogPreferences(
                    defaultScreen = defaultScreen ?: MikroblogScreen.Newest,
                    cutLongEntries = cutLongEntries ?: true,
                    // Domyslnie spoilery rozwijaja sie inline; popup jest opcja (opt-in).
                    openSpoilersInDialog = openSpoilersInDialog ?: false,
                    // Zageszczenie listy - opt-in.
                    showTopComments = showTopComments ?: false,
                )
            }
    }

internal data class MikroblogPreferences(
    val defaultScreen: MikroblogScreen,
    val cutLongEntries: Boolean,
    val openSpoilersInDialog: Boolean,
    val showTopComments: Boolean,
)

internal enum class MikroblogScreen {
    Active,
    Newest,
    SixHours,
    TwelveHours,
    TwentyFourHours,
}

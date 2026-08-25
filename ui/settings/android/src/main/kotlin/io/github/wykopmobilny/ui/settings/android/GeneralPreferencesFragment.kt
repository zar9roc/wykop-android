package io.github.wykopmobilny.ui.settings.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceFragmentCompat
import io.github.wykopmobilny.ui.settings.GeneralPreferencesUi.NotificationsUi.RefreshPeriodUi
import io.github.wykopmobilny.ui.settings.ListSetting
import io.github.wykopmobilny.ui.settings.GetGeneralPreferences
import io.github.wykopmobilny.ui.settings.SettingsDependencies
import io.github.wykopmobilny.utils.requireDependency
import kotlinx.coroutines.launch

internal class GeneralPreferencesFragment : PreferenceFragmentCompat() {
    lateinit var getGeneralPreferences: GetGeneralPreferences

    override fun onAttach(context: Context) {
        getGeneralPreferences = context.requireDependency<SettingsDependencies>().general()
        super.onAttach(context)
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.general_preferences, rootKey)
        bindPreference("exportLogs", ::exportLogs)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                getGeneralPreferences().collect {
                    bindPreference("appearance", ::openAppearanceSettings)
                    bindCheckbox("showNotifications", it.notifications.notificationsEnabled)
                    bindCheckbox("disableExitConfirmation", it.notifications.exitConfirmation)
                    bindCheckbox("showAdultContent", it.filtering.showPlus18Content)
                    bindCheckbox("hideNsfw", it.filtering.hideNsfwContent)
                    bindCheckbox("hideLowRangeAuthors", it.filtering.hideNewUserContent)
                    bindCheckbox("hideContentWithoutTags", it.filtering.hideContentWithNoTags)
                    bindCheckbox("hideBlacklistedViews", it.filtering.hideBlacklistedContent)
                    bindPreference("blacklist", it.filtering.manageBlackList)
                    bindCheckbox("useBuiltInBrowser", it.filtering.useEmbeddedBrowser)
                    bindPreference("clearhistory", it.filtering.clearSearchHistory)
                    bindList(
                        key = "notificationsSchedulerDelay",
                        setting = withFrequentPollingDialog(it.notifications.notificationRefreshPeriod),
                        mapping = refreshPeriodMapping,
                    )
                }
            }
        }
    }

    // Okresy < 15 min (foreground service): wybor pokazuje dialog wyjasniajacy.
    private val frequentPeriods = setOf(RefreshPeriodUi.OneMinute, RefreshPeriodUi.FiveMinutes)

    private fun withFrequentPollingDialog(setting: ListSetting<RefreshPeriodUi>) =
        ListSetting(
            values = setting.values,
            currentValue = setting.currentValue,
            isEnabled = setting.isEnabled,
            onSelected = { period ->
                if (period in frequentPeriods && setting.currentValue !in frequentPeriods) {
                    androidx.appcompat.app.AlertDialog
                        .Builder(requireContext())
                        .setTitle(R.string.frequent_polling_dialog_title)
                        .setMessage(R.string.frequent_polling_dialog_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                setting.onSelected(period)
            },
        )

    private val refreshPeriodMapping by lazy {
        RefreshPeriodUi.entries.associateWith { period ->
            when (period) {
                RefreshPeriodUi.OneMinute -> R.string.preferences_notification_period_1_minute
                RefreshPeriodUi.FiveMinutes -> R.string.preferences_notification_period_5_minutes
                RefreshPeriodUi.FifteenMinutes -> R.string.preferences_notification_period_15_minutes
                RefreshPeriodUi.ThirtyMinutes -> R.string.preferences_notification_period_30_minutes
                RefreshPeriodUi.OneHour -> R.string.preferences_notification_period_1_hour
                RefreshPeriodUi.TwoHours -> R.string.preferences_notification_period_2_hours
                RefreshPeriodUi.FourHours -> R.string.preferences_notification_period_4_hours
                RefreshPeriodUi.EightHours -> R.string.preferences_notification_period_8_hours
            }.let { resources.getString(it) }
        }
    }

    // Udostepnia pliki logow przez systemowy share sheet. Lokalizacja jak
    // w FileLogAntilog (app/initializers) - modul settings nie widzi tamtej
    // klasy, wiec sciezka jest swiadomie zduplikowana.
    private fun exportLogs() {
        val context = requireContext()
        val logDir = (context.getExternalFilesDir(null) ?: context.filesDir).resolve("crashlogs")
        val logs =
            listOf("log.txt", "log.1.txt")
                .map(logDir::resolve)
                .filter { it.exists() && it.length() > 0 }
        if (logs.isEmpty()) {
            Toast.makeText(context, R.string.export_logs_empty, Toast.LENGTH_LONG).show()
            return
        }
        val uris =
            logs.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        val intent =
            if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uris.single()) }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
            }.apply {
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        startActivity(Intent.createChooser(intent, getString(R.string.pref_export_logs)))
    }

    private fun openAppearanceSettings() {
        parentFragmentManager
            .beginTransaction()
            .replace(R.id.container, AppearancePreferencesFragment())
            .addToBackStack(null)
            .commit()
    }
}

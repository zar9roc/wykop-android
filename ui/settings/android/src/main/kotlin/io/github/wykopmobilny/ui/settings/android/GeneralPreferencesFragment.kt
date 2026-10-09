package io.github.wykopmobilny.ui.settings.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import io.github.wykopmobilny.ui.settings.android.databinding.DialogCustomApiKeyBinding
import io.github.wykopmobilny.ui.settings.GeneralPreferencesUi
import io.github.wykopmobilny.ui.settings.GeneralPreferencesUi.NotificationsUi.RefreshPeriodUi
import io.github.wykopmobilny.ui.settings.ListSetting
import io.github.wykopmobilny.ui.settings.GetGeneralPreferences
import io.github.wykopmobilny.ui.settings.SettingsDependencies
import io.github.wykopmobilny.ui.settings.AppUpdates
import io.github.wykopmobilny.ui.settings.UpdateCheckFrequency
import io.github.wykopmobilny.ui.settings.UpdateCheckResult
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
        bindUpdates()

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
                    bindApiKey("customApiKey", it.advanced.apiKey)
                }
            }
        }
    }

    private val appUpdates: AppUpdates?
        get() = context?.applicationContext as? AppUpdates

    private fun bindUpdates() {
        val updates = appUpdates
        findPreference<PreferenceCategory>("updatesCategory")?.isVisible = updates != null
        updates ?: return
        bindList(
            key = "updateCheckFrequency",
            setting =
                ListSetting(
                    values = UpdateCheckFrequency.entries,
                    currentValue = updates.checkFrequency,
                    onSelected = { frequency ->
                        updates.checkFrequency = frequency
                        bindUpdates()
                    },
                ),
            mapping = updateFrequencyMapping,
        )
        val checkNow = findPreference<Preference>("checkUpdateNow") ?: return
        checkNow.summary = getString(R.string.pref_check_update_installed, updates.installedVersion)
        checkNow.setOnPreferenceClickListener {
            checkNow.isEnabled = false
            checkNow.setSummary(R.string.pref_check_update_checking)
            viewLifecycleOwner.lifecycleScope.launch {
                val result = updates.checkNow()
                checkNow.isEnabled = true
                checkNow.summary = getString(R.string.pref_check_update_installed, updates.installedVersion)
                showUpdateResult(result)
            }
            true
        }
    }

    private fun showUpdateResult(result: UpdateCheckResult) {
        val context = context ?: return
        when (result) {
            is UpdateCheckResult.Available ->
                AlertDialog
                    .Builder(context)
                    .setTitle(R.string.update_available_dialog_title)
                    .setMessage(getString(R.string.update_available_dialog_message, result.version))
                    .setPositiveButton(R.string.update_available_dialog_open) { _, _ -> openDownloadPage(result.downloadUrl) }
                    .setNegativeButton(R.string.update_available_dialog_later, null)
                    .show()

            UpdateCheckResult.UpToDate -> Toast.makeText(context, R.string.update_up_to_date, Toast.LENGTH_SHORT).show()

            UpdateCheckResult.Failed -> Toast.makeText(context, R.string.update_check_failed, Toast.LENGTH_LONG).show()
        }
    }

    // Zawsze zewnetrzna przegladarka (niezaleznie od "wbudowanej") - tam pobierze sie APK.
    private fun openDownloadPage(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(requireContext(), R.string.update_open_browser_failed, Toast.LENGTH_LONG).show() }
    }

    private val updateFrequencyMapping by lazy {
        UpdateCheckFrequency.entries.associateWith { frequency ->
            when (frequency) {
                UpdateCheckFrequency.Daily -> R.string.pref_update_frequency_daily
                UpdateCheckFrequency.Weekly -> R.string.pref_update_frequency_weekly
                UpdateCheckFrequency.Never -> R.string.pref_update_frequency_never
            }.let { resources.getString(it) }
        }
    }

    private fun bindApiKey(
        key: String,
        setting: GeneralPreferencesUi.AdvancedUi.ApiKeyUi,
    ) {
        val pref = findPreference<Preference>(key) ?: return
        pref.setSummary(
            if (setting.currentKey.isNullOrBlank()) {
                R.string.custom_api_key_summary_builtin
            } else {
                R.string.custom_api_key_summary_custom
            },
        )
        pref.setOnPreferenceClickListener {
            showApiKeyDialog(setting)
            true
        }
    }

    /**
     * Okienko wlasnego klucza API: pola klucz+sekret, przycisk testu i 3 akcje
     * (Anuluj / Domyślne / Zapisz). Zapis wymaga pozytywnego testu wpisanej pary;
     * kazda zmiana w polach uniewaznia test. Puste pola = powrot do klucza
     * wbudowanego - wtedy zapis bez testu.
     */
    private fun showApiKeyDialog(setting: GeneralPreferencesUi.AdvancedUi.ApiKeyUi) {
        val binding = DialogCustomApiKeyBinding.inflate(layoutInflater)
        binding.apiKeyInput.setText(setting.currentKey.orEmpty())
        // Sekret nigdy nie jest prefillowany - raz zapisany jest nie do odzyskania
        // z UI; zmiana klucza wymaga wpisania go od nowa.

        val dialog =
            AlertDialog
                .Builder(requireContext())
                .setTitle(R.string.pref_custom_api_key)
                .setView(binding.root)
                .setPositiveButton(R.string.custom_api_key_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.custom_api_key_use_defaults) { _, _ -> setting.saveAction(null, null) }
                .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            // Para, ktora przeszla test - zapis wlasnego klucza dozwolony tylko dla niej.
            var validatedPair: Pair<String, String>? = null

            fun currentInput(): Pair<String, String> {
                val key =
                    binding.apiKeyInput.text
                        .toString()
                        .trim()
                val secret =
                    binding.apiSecretInput.text
                        .toString()
                        .trim()
                return key to secret
            }

            fun refreshButtons() {
                val input = currentInput()
                val bothBlank = input.first.isBlank() && input.second.isBlank()
                binding.testButton.isEnabled = input.first.isNotBlank() && input.second.isNotBlank()
                saveButton.isEnabled = bothBlank || input == validatedPair
            }

            val onEdited: (android.text.Editable?) -> Unit = {
                binding.testStatus.text = ""
                refreshButtons()
            }
            binding.apiKeyInput.doAfterTextChanged(onEdited)
            binding.apiSecretInput.doAfterTextChanged(onEdited)

            binding.testButton.setOnClickListener {
                val input = currentInput()
                binding.testButton.isEnabled = false
                binding.testStatus.setText(R.string.custom_api_key_testing)
                lifecycleScope.launch {
                    val isValid = setting.testCredentials(input.first, input.second)
                    if (isValid) validatedPair = input
                    binding.testStatus.setText(
                        if (isValid) R.string.custom_api_key_valid else R.string.custom_api_key_invalid,
                    )
                    refreshButtons()
                }
            }

            saveButton.setOnClickListener {
                val input = currentInput()
                setting.saveAction(input.first.takeIf { it.isNotBlank() }, input.second.takeIf { it.isNotBlank() })
                dialog.dismiss()
            }

            refreshButtons()
        }
        dialog.show()
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

package io.github.wykopmobilny.ui.modules.notificationslist.notification

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.fragments.DataFragment
import io.github.wykopmobilny.models.fragments.PagedDataModel
import io.github.wykopmobilny.models.fragments.getDataFragmentInstance
import io.github.wykopmobilny.models.fragments.removeDataFragment
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.NotificationsListAdapter
import io.github.wykopmobilny.ui.modules.notificationslist.BaseNotificationsListFragment
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import javax.inject.Inject

class NotificationsListFragment : BaseNotificationsListFragment() {
    companion object {
        const val DATA_FRAGMENT_TAG = "NOTIFICATIONS_LIST_ACTIVITY"

        fun newInstance() = NotificationsListFragment()
    }

    @Inject
    override lateinit var linkHandler: WykopLinkHandler

    @Inject
    override lateinit var notificationAdapter: NotificationsListAdapter

    @Inject
    lateinit var presenter: NotificationsListPresenter

    @Inject
    lateinit var settingsApi: SettingsPreferencesApi

    private lateinit var entryFragmentData: DataFragment<PagedDataModel<List<Notification>>>

    /**
     * Wywolywane WYLACZNIE przez klikniecie stopki "pokaz starsze" - scrollowanie
     * do konca listy niczego nie doladowuje. Jedno klikniecie = jedna strona.
     */
    override fun loadMore() {
        if (settingsApi.groupNotifications) {
            presenter.loadGrouped(false)
        } else {
            presenter.loadData(false)
        }
    }

    override fun onRefresh() {
        if (settingsApi.groupNotifications) {
            presenter.loadGrouped(true)
        } else {
            presenter.loadData(true)
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        presenter.subscribe(this)

        entryFragmentData = supportFragmentManager.getDataFragmentInstance(DATA_FRAGMENT_TAG)
        if (entryFragmentData.data != null && entryFragmentData.data!!.model.isNotEmpty()) {
            binding.loadingView.isVisible = false
            presenter.page = entryFragmentData.data!!.page
            // Prezenter odzyskuje surowe powiadomienia (bez wierszy zbiorczych),
            // zeby stan po obrocie ekranu zgadzal sie z tym, co pokazuje lista -
            // i zeby odtworzenie nie kosztowalo dodatkowego zapytania.
            presenter.restoreLoaded(entryFragmentData.data!!.model)
            notificationAdapter.addData(entryFragmentData.data!!.model, true)
            // Stopka zostaje widoczna - po obrocie ekranu nie wiemy, czy byla to
            // ostatnia strona, wiec pokazujemy ja optymistycznie (pierwsze pobranie
            // pustej strony i tak ja zdejmie). Zadnego zapytania sprawdzajacego.
            markInitialLoadDone()
        } else {
            startInitialLoadIfVisible()
        }
    }

    override fun onDestroyView() {
        presenter.unsubscribe()
        super.onDestroyView()
    }

    override fun markAsRead() = presenter.readNotifications()

    override fun onPause() {
        super.onPause()
        if (isRemoving) supportFragmentManager.removeDataFragment(entryFragmentData)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        entryFragmentData.data = PagedDataModel(presenter.page, notificationAdapter.data)
    }
}

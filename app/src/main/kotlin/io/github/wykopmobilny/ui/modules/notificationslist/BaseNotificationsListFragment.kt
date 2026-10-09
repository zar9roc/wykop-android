package io.github.wykopmobilny.ui.modules.notificationslist

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.BaseFragment
import io.github.wykopmobilny.databinding.ActivityNotificationsListBinding
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.ui.adapters.NotificationsListAdapter
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.prepare
import io.github.wykopmobilny.utils.viewBinding
import io.github.wykopmobilny.utils.showLoadMoreErrorSnackbar

abstract class BaseNotificationsListFragment :
    BaseFragment(R.layout.activity_notifications_list),
    NotificationsListView,
    SwipeRefreshLayout.OnRefreshListener {
    protected val binding by viewBinding(ActivityNotificationsListBinding::bind)
    abstract var notificationAdapter: NotificationsListAdapter
    abstract var linkHandler: WykopLinkHandler

    abstract fun markAsRead()

    abstract fun loadMore()

    // Ekran powiadomien to ViewPager, ktory tworzy OBA fragmenty od razu. Zapytanie
    // startuje dopiero, gdy zakladka naprawde staje sie widoczna - wejscie na ekran
    // kosztuje jedno zapytanie (widoczna zakladka), a nie po jednym na kazda z dwoch.
    private var initialLoadDone = false

    /** Dane odtworzone po obrocie ekranu - nie wolno ich pobierac drugi raz. */
    protected fun markInitialLoadDone() {
        initialLoadDone = true
    }

    /**
     * Pierwsze ladowanie zakladki - dokladnie raz, niezaleznie od tego, czy wyzwoli je
     * [onViewCreated] (zakladka widoczna od poczatku) czy [setUserVisibleHint]
     * (uzytkownik przelaczyl sie na nia pozniej). Powrot na zakladke nie strzela ponownie.
     */
    protected fun startInitialLoadIfVisible() {
        if (initialLoadDone || !userVisibleHint || view == null) return
        initialLoadDone = true
        binding.loadingView.isVisible = true
        onRefresh()
    }

    @Suppress("DEPRECATION")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser) startInitialLoadIfVisible()
    }

    private fun onNotificationClicked(position: Int) {
        val notification = notificationAdapter.data[position]
        notification.new = false
        notificationAdapter.notifyDataSetChanged()
        Napier.d("Notification url=${notification.url}")
        notification.url?.let { linkHandler.handleUrl(it, true) }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.swiperefresh.setOnRefreshListener(this)

        // Stopka "pokaz starsze" - jedyny sposob na kolejna strone. NIE podpinamy
        // loadNewDataListener (callbacku EndlessScrollListener), wiec samo scrollowanie
        // do konca listy niczego nie wyzwala.
        notificationAdapter.loadMoreListener = {
            loadMore()
        }

        notificationAdapter.itemClickListener = { onNotificationClicked(it) }
        binding.recyclerView.apply {
            prepare()
            adapter = notificationAdapter
        }
        binding.swiperefresh.isRefreshing = false
    }

    override fun addNotifications(
        notifications: List<Notification>,
        shouldClearAdapter: Boolean,
    ) {
        binding.loadingView.isVisible = false
        binding.swiperefresh.isRefreshing = false
        notificationAdapter.addData(
            if (!shouldClearAdapter) notifications.filterNot { notificationAdapter.data.contains(it) } else notifications,
            shouldClearAdapter,
        )
    }

    override fun showReadToast() {
        onRefresh()
        Toast.makeText(context, R.string.read_notifications, Toast.LENGTH_SHORT).show()
    }

    /**
     * Blad doladowania kolejnej strony: snackbar z "Ponow" zamiast modalu, a stopka
     * wraca do stanu klikalnego (lista zostaje taka, jaka byla - nic nie znika).
     */
    override fun showLoadMoreError(e: Throwable) {
        notificationAdapter.finishLoadMore()
        showLoadMoreErrorSnackbar(e) { notificationAdapter.requestLoadMore() }
    }

    override fun disableLoading() = notificationAdapter.disableLoading()
}

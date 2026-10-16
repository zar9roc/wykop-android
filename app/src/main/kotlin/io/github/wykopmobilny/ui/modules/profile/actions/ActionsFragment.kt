package io.github.wykopmobilny.ui.modules.profile.actions

import android.os.Bundle
import android.view.View
import io.github.wykopmobilny.base.BaseEntryLinkFragment
import io.github.wykopmobilny.ui.modules.profile.ProfileActivity
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import javax.inject.Inject

/**
 * Lista mieszana profilu: wpisy/znaleziska z wierszami pod nimi (komentarze, powiazane).
 * Ten sam ekran obsluguje zakladke Akcje oraz podzakladki Znaleziska > Komentarze
 * i Powiazane - roznia sie tylko zrodlem danych ([ActionsFragmentPresenter.Source]).
 */
class ActionsFragment :
    BaseEntryLinkFragment(),
    ActionsView {
    companion object {
        private const val ARG_SOURCE = "SOURCE"

        fun newInstance(source: ActionsFragmentPresenter.Source = ActionsFragmentPresenter.Source.ACTIONS) =
            ActionsFragment().apply {
                arguments = android.os.Bundle().apply { putString(ARG_SOURCE, source.name) }
            }
    }

    @Inject
    lateinit var userManager: UserManagerApi

    @Inject
    lateinit var presenter: ActionsFragmentPresenter

    override var loadDataListener: (Boolean) -> Unit = { presenter.getActions(it) }
    private val username by lazy { (activity as ProfileActivity).username }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        presenter.subscribe(this)
        presenter.username = username
        presenter.source =
            arguments
                ?.getString(ARG_SOURCE)
                ?.let(ActionsFragmentPresenter.Source::valueOf)
                ?: ActionsFragmentPresenter.Source.ACTIONS
        // Na profilu podglad najlepszych komentarzy jest zawsze wlaczony (bez ustawienia).
        entriesAdapter.forceShowTopComments = true
        entriesAdapter.entryActionListener = presenter
        entriesAdapter.linkActionListener = presenter
        entriesAdapter.loadNewDataListener = { loadDataListener(false) }
        loadDataListener(true)
    }

    override fun onDestroyView() {
        presenter.unsubscribe()
        super.onDestroyView()
    }
}

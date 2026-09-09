package io.github.wykopmobilny.ui.modules.profile.microblog.voted

import android.os.Bundle
import android.view.View
import io.github.wykopmobilny.base.BaseEntriesFragment
import io.github.wykopmobilny.ui.modules.profile.ProfileActivity
import io.github.wykopmobilny.ui.modules.profile.microblog.entries.MicroblogEntriesPresenter
import io.github.wykopmobilny.ui.modules.profile.microblog.entries.MicroblogEntriesView
import javax.inject.Inject

/**
 * Zakladka "Plusowane" na profilu (mikroblog) - wpisy zaplusowane przez uzytkownika
 * (GET /profile/users/{username}/entries/voted). Ten sam prezenter co "Wpisy",
 * skonfigurowany na zrodlo VOTED w [MicroblogVotedEntriesModule].
 */
class MicroblogVotedEntriesFragment :
    BaseEntriesFragment(),
    MicroblogEntriesView {
    companion object {
        fun newInstance() = MicroblogVotedEntriesFragment()
    }

    @Inject
    lateinit var presenter: MicroblogEntriesPresenter
    private val username by lazy { (activity as ProfileActivity).username }
    override var loadDataListener: (Boolean) -> Unit = { presenter.loadData(it) }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        presenter.subscribe(this)
        presenter.username = username
        entriesAdapter.entryActionListener = presenter
        entriesAdapter.loadNewDataListener = { loadDataListener(false) }
        presenter.loadData(true)
    }

    override fun onDestroyView() {
        presenter.unsubscribe()
        super.onDestroyView()
    }
}

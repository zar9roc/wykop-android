package io.github.wykopmobilny.ui.modules.profile.microblog.comments

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.BaseFragment
import io.github.wykopmobilny.databinding.DialogVotersBinding
import io.github.wykopmobilny.databinding.EntriesFragmentBinding
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.Voter
import io.github.wykopmobilny.ui.adapters.ProfileCommentsAdapter
import io.github.wykopmobilny.ui.dialogs.VotersDialogListener
import io.github.wykopmobilny.ui.dialogs.createVotersDialogListener
import io.github.wykopmobilny.ui.modules.profile.ProfileActivity
import io.github.wykopmobilny.utils.prepare
import io.github.wykopmobilny.utils.viewBinding
import javax.inject.Inject

/**
 * Zakladka "Komentarze" na profilu. Lista jest mieszana: wpis, pod ktorym padl
 * komentarz, a zaraz za nim komentarz(e) uzytkownika - tak jak wyglada to na ekranie
 * wpisu. "Odpowiedz"/"Cytuj" pod komentarzem przenosi na ekran wpisu z gotowa trescia
 * w polu odpowiedzi (patrz ProfileCommentsAdapter).
 */
class MicroblogCommentsFragment :
    BaseFragment(R.layout.entries_fragment),
    MicroblogCommentsView,
    SwipeRefreshLayout.OnRefreshListener {
    companion object {
        fun newInstance() = MicroblogCommentsFragment()
    }

    @Inject
    lateinit var presenter: MicroblogCommentsPresenter

    @Inject
    lateinit var commentsAdapter: ProfileCommentsAdapter

    private val binding by viewBinding(EntriesFragmentBinding::bind)
    private lateinit var votersDialogListener: VotersDialogListener

    private val username by lazy { (activity as ProfileActivity).username }
    private val loadDataListener: (Boolean) -> Unit = { presenter.loadData(it) }

    override var showSearchEmptyView: Boolean
        get() = binding.empty.searchEmptyView.isVisible
        set(value) {
            binding.empty.searchEmptyView.isVisible = value
            if (value) {
                commentsAdapter.addData(emptyList(), true)
                commentsAdapter.disableLoading()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        presenter.subscribe(this)
        presenter.username = username
        commentsAdapter.entryActionListener = presenter
        commentsAdapter.entryCommentActionListener = presenter
        commentsAdapter.loadNewDataListener = { loadDataListener(false) }

        binding.swipeRefresh.setOnRefreshListener(this)
        binding.recyclerView.run {
            prepare()
            adapter = commentsAdapter
        }
        binding.loadingView.isVisible = true

        presenter.loadData(true)
    }

    override fun onDestroyView() {
        presenter.unsubscribe()
        super.onDestroyView()
    }

    override fun onRefresh() = loadDataListener(true)

    override fun addItems(
        items: List<EntryListRow>,
        shouldRefresh: Boolean,
    ) {
        commentsAdapter.addData(items, shouldRefresh)
        binding.swipeRefresh.isRefreshing = false
        binding.loadingView.isVisible = false

        if (shouldRefresh) {
            (binding.recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
        }
    }

    override fun disableLoading() {
        commentsAdapter.disableLoading()
        binding.swipeRefresh.isRefreshing = false
        binding.loadingView.isVisible = false
        // Pusta pierwsza strona: addItems() nie zostanie wywolane - chowamy loader
        // i pokazujemy pusty stan zamiast wiecznego kreciolka.
        showSearchEmptyView = commentsAdapter.data.isEmpty()
    }

    override fun updateComment(comment: EntryComment) = commentsAdapter.updateComment(comment)

    override fun updateEntry(entry: Entry) = commentsAdapter.updateEntry(entry)

    override fun showVoters(voters: List<Voter>) = votersDialogListener(voters)

    override fun openVotersMenu() {
        val dialog = BottomSheetDialog(requireActivity())
        val votersDialogView = DialogVotersBinding.inflate(layoutInflater)
        votersDialogView.votersTextView.isVisible = false
        dialog.setContentView(votersDialogView.root)
        votersDialogListener = createVotersDialogListener(dialog, votersDialogView)
        dialog.show()
    }
}

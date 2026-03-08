package io.github.wykopmobilny.base

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.databinding.EntriesFragmentBinding
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.ui.adapters.LinksAdapter
import io.github.wykopmobilny.ui.fragments.links.LinksFragmentView
import io.github.wykopmobilny.utils.prepare
import io.github.wykopmobilny.utils.viewBinding
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

open class BaseLinksFragment :
    BaseFragment(R.layout.entries_fragment),
    LinksFragmentView,
    SwipeRefreshLayout.OnRefreshListener {
    @Inject
    lateinit var linksApi: LinksApi

    @Inject
    lateinit var linksAdapter: LinksAdapter

    protected val binding by viewBinding(EntriesFragmentBinding::bind)

    override var showSearchEmptyView: Boolean
        get() = binding.empty.searchEmptyView.isVisible
        set(value) {
            binding.empty.searchEmptyView.isVisible = value
        }

    open var loadDataListener: (Boolean) -> Unit = {}

    private val subjectDisposable = CompositeDisposable()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        linksAdapter.loadNewDataListener = { loadDataListener(false) }

        // Setup views
        binding.swipeRefresh.setOnRefreshListener(this)
        binding.recyclerView.run {
            prepare()
            adapter = linksAdapter
        }

        binding.loadingView.isVisible = true

        val schedulers = WykopSchedulers()
        subjectDisposable.addAll(
            linksApi.digSubject
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe { updateLinkVoteState(it.linkId, it.userVote) },
            linksApi.burySubject
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe { updateLinkVoteState(it.linkId, it.userVote) },
            linksApi.voteRemoveSubject
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe { updateLinkVoteState(it.linkId, it.userVote) },
        )
    }

    override fun onDestroyView() {
        subjectDisposable.clear()
        super.onDestroyView()
    }

    private fun updateLinkVoteState(
        linkId: Long,
        userVote: String?,
    ) {
        linksAdapter.data.firstOrNull { it.id == linkId }?.apply {
            when {
                userVote == "dig" && this.userVote != "dig" -> {
                    this.voteCount += 1
                    if (this.userVote == "bury") this.buryCount -= 1
                }

                userVote == "bury" && this.userVote != "bury" -> {
                    this.buryCount += 1
                    if (this.userVote == "dig") this.voteCount -= 1
                }

                userVote == null -> {
                    if (this.userVote == "dig") {
                        this.voteCount -= 1
                    } else if (this.userVote == "bury") {
                        this.buryCount -= 1
                    }
                }
            }
            this.userVote = userVote
            linksAdapter.updateLink(this)
        }
    }

    /**
     * Removes progressbar from adapter
     */
    override fun disableLoading() = linksAdapter.disableLoading()

    /**
     * Use this function to add items to EntriesFragment
     * @param items List of entries to add
     * @param shouldRefresh If true adapter will refresh its data with provided items. False by default
     */
    override fun addItems(
        items: List<Link>,
        shouldRefresh: Boolean,
    ) {
        linksAdapter.addData(items, shouldRefresh)
        binding.swipeRefresh.isRefreshing = false
        binding.loadingView.isVisible = false

        // Scroll to top if refreshing list
        if (shouldRefresh) {
            (binding.recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
        }
    }

    override fun updateLink(link: Link) = linksAdapter.updateLink(link)

    override fun onRefresh() = loadDataListener(true)
}

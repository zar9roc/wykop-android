package io.github.wykopmobilny.ui.modules.tag.links

import android.os.Bundle
import android.view.View
import io.github.wykopmobilny.base.BaseLinksFragment
import io.github.wykopmobilny.ui.modules.tag.TagActivity
import io.github.wykopmobilny.ui.modules.tag.TagFilterableFragment
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import javax.inject.Inject

class TagLinksFragment :
    BaseLinksFragment(),
    TagLinksView,
    TagFilterableFragment {
    companion object {
        const val DATA_FRAGMENT_TAG = "TAG_DATA_FRAGMENT"
        const val EXTRA_TAG = "EXTRA_TAG"

        fun newInstance(tag: String): androidx.fragment.app.Fragment {
            val fragment = TagLinksFragment()
            val data = Bundle()
            data.putString(EXTRA_TAG, tag)
            fragment.arguments = data
            return fragment
        }
    }

    @Inject
    lateinit var presenter: TagLinksPresenter

    @Inject
    lateinit var userManager: UserManagerApi

    override var loadDataListener: (Boolean) -> Unit = { presenter.loadData(it) }

    private val tagString by lazy { requireArguments().getString(EXTRA_TAG) }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        presenter.subscribe(this)
        presenter.tag = tagString!!
        // Zakladka moze zostac utworzona juz po ustawieniu filtra na drugiej
        // zakladce - dociagamy aktualny stan z aktywnosci przed pierwszym ladowaniem.
        (activity as? TagActivity)?.let {
            presenter.sort = it.tagSort
            presenter.year = it.tagArchiveYear
            presenter.month = it.tagArchiveMonth
        }
        linksAdapter.linksActionListener = presenter
        linksAdapter.loadNewDataListener = { loadDataListener(false) }
        presenter.loadData(true)
    }

    override fun applyTagFilter(
        sort: String,
        year: Int?,
        month: Int?,
    ) {
        presenter.sort = sort
        presenter.year = year
        presenter.month = month
        // Kreciolek do czasu, az przefiltrowana lista sie zaladuje (addItems/disableLoading go chowa).
        if (view != null) {
            binding.swipeRefresh.isRefreshing = true
        }
        presenter.loadData(true)
    }

    override fun onDestroyView() {
        presenter.unsubscribe()
        super.onDestroyView()
    }
}

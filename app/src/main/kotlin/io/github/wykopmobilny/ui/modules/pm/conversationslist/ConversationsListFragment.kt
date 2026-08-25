package io.github.wykopmobilny.ui.modules.pm.conversationslist

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.BaseActivity
import io.github.wykopmobilny.base.BaseFragment
import io.github.wykopmobilny.databinding.ActivityConversationsListBinding
import io.github.wykopmobilny.models.dataclass.Conversation
import io.github.wykopmobilny.ui.adapters.ConversationsListAdapter
import io.github.wykopmobilny.ui.helpers.EndlessScrollListener
import io.github.wykopmobilny.utils.hideKeyboard
import io.github.wykopmobilny.utils.prepare
import io.github.wykopmobilny.utils.viewBinding
import javax.inject.Inject

class ConversationsListFragment :
    BaseFragment(R.layout.activity_conversations_list),
    ConversationsListView,
    SwipeRefreshLayout.OnRefreshListener {
    @Inject
    lateinit var presenter: ConversationsListPresenter

    private val binding by viewBinding(ActivityConversationsListBinding::bind)

    private val conversationsAdapter by lazy { ConversationsListAdapter() }

    private val searchHandler = Handler(Looper.getMainLooper())

    private var searchItem: MenuItem? = null
    private var isSearchActive = false

    companion object {
        const val DATA_FRAGMENT_TAG = "CONVERSATIONS_LIST"
        private const val SEARCH_DEBOUNCE_MS = 500L

        fun newInstance() = ConversationsListFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (activity as BaseActivity).supportActionBar?.setTitle(R.string.messages)
        binding.swiperefresh.setOnRefreshListener(this)

        binding.recyclerView.apply {
            prepare()
            adapter = conversationsAdapter
            (layoutManager as? LinearLayoutManager)?.let { layoutManager ->
                addOnScrollListener(EndlessScrollListener(layoutManager) { presenter.loadMore() })
            }
        }
        // Wyszukiwanie rozmow po nicku (query, min 3 znaki) - z debounce, zeby nie
        // odpytywac API przy kazdym znaku. Presenter sam pilnuje minimalnej dlugosci.
        binding.searchEditText.doAfterTextChanged { text ->
            searchHandler.removeCallbacksAndMessages(null)
            searchHandler.postDelayed({ presenter.loadConversations(text?.toString()) }, SEARCH_DEBOUNCE_MS)
        }
        binding.swiperefresh.isRefreshing = false
        presenter.subscribe(this)

        binding.loadingView.isVisible = true
        onRefresh()
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.conversations_list_menu, menu)
        searchItem = menu.findItem(R.id.action_search)
        applySearchIconState()
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_search -> {
                toggleSearch()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    // Lupa: pokazuje pole filtrowania (ikona zmienia sie na X). X: chowa pole,
    // czysci filtr i wraca do widoku wszystkich rozmow.
    private fun toggleSearch() {
        isSearchActive = !isSearchActive
        if (isSearchActive) {
            binding.searchEditText.isVisible = true
            binding.searchEditText.requestFocus()
            showKeyboard(binding.searchEditText)
        } else {
            searchHandler.removeCallbacksAndMessages(null)
            binding.searchEditText.setText("")
            binding.searchEditText.isVisible = false
            activity?.hideKeyboard()
            presenter.loadConversations(null)
        }
        applySearchIconState()
    }

    private fun applySearchIconState() {
        searchItem?.apply {
            // Toolbar PM ma stale ciemne tlo (colorPrimaryDark) - ikona musi byc biala w kazdym
            // motywie. Zamiast wariantu _dark: bazowa ikona + biel z ?attr-owego wzorca
            // (@color/icon_on_colored_surface) nalozona przez iconTint (nadpisuje wbudowany tint).
            setIcon(if (isSearchActive) R.drawable.ic_close else R.drawable.ic_search)
            MenuItemCompat.setIconTintList(this, ColorStateList.valueOf(Color.WHITE))
            setTitle(if (isSearchActive) R.string.close else R.string.search)
        }
    }

    private fun showKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    // Celowo NIE odswiezamy listy w onResume: widok fragmentu przezywa powrot z pojedynczej
    // rozmowy, wiec zachowujemy zaladowane strony i pozycje scrolla (pelny reload scrollowal
    // na gore i wymuszal ponowne doladowanie). Reczny refresh -> swipe-to-refresh.
    override fun onDestroyView() {
        searchHandler.removeCallbacksAndMessages(null)
        searchItem = null
        presenter.unsubscribe()
        super.onDestroyView()
    }

    override fun showConversations(conversations: List<Conversation>) {
        binding.loadingView.isVisible = false
        binding.swiperefresh.isRefreshing = false
        conversationsAdapter.apply {
            items.clear()
            items.addAll(conversations)
            notifyDataSetChanged()
        }
        binding.recyclerView.scrollToPosition(0)
    }

    override fun appendConversations(conversations: List<Conversation>) {
        val start = conversationsAdapter.items.size
        conversationsAdapter.items.addAll(conversations)
        conversationsAdapter.notifyItemRangeInserted(start, conversations.size)
    }

    // Swipe-refresh i wejscie zachowuja biezacy filtr wyszukiwania.
    override fun onRefresh() = presenter.loadConversations()
}

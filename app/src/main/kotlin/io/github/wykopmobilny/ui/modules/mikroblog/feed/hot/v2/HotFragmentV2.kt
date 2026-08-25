package io.github.wykopmobilny.ui.modules.mikroblog.feed.hot.v2

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.android.support.AndroidSupportInjection
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.databinding.DialogVotersBinding
import io.github.wykopmobilny.databinding.FragmentHotV2Binding
import io.github.wykopmobilny.base.BaseNavigationView
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedComponent
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedKey
import io.github.wykopmobilny.entries.feed.MicroblogFeedSort
import io.github.wykopmobilny.entries.feed.MicroblogFeedUi
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.ui.dialogs.VotersDialogListener
import io.github.wykopmobilny.ui.dialogs.createVotersDialogListener
import io.github.wykopmobilny.ui.fragments.entries.EntriesInteractor
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.modules.NavigatorApi
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationInterface
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.dialogs.showExceptionDialog
import io.github.wykopmobilny.utils.InjectableViewModel
import io.github.wykopmobilny.utils.bindings.collectErrorDialog
import io.github.wykopmobilny.utils.bindings.collectSwipeRefresh
import io.github.wykopmobilny.utils.intoComposite
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.prepare
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.github.wykopmobilny.utils.viewModelWrapperFactoryKeyed
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Feed "Gorące" na nowym stacku (domain + coroutines, ręczny pager in-memory).
 * Renderowanie i mutacje reużywają sprawdzone EntryViewHolder + EntriesInteractor
 * (jak port detalu wpisu) - port dotyczy warstwy danych/paginacji, nie repaintu.
 */
class HotFragmentV2 :
    Fragment(R.layout.fragment_hot_v2),
    BaseNavigationView,
    EntryActionListener {
    @Inject
    lateinit var entriesInteractor: EntriesInteractor

    @Inject
    lateinit var entriesApi: EntriesApi

    @Inject
    lateinit var schedulers: Schedulers

    @Inject
    lateinit var settingsPreferences: SettingsPreferencesApi

    @Inject
    lateinit var userManagerApi: UserManagerApi

    @Inject
    lateinit var navigatorApi: NavigatorApi

    @Inject
    lateinit var owmContentFilter: OWMContentFilter

    private val navigation by lazy { requireActivity() as MainNavigationInterface }
    private val navigator by lazy { NewNavigator(requireActivity(), settingsPreferences) }
    private val linkHandler by lazy { WykopLinkHandler(requireActivity(), navigator) }
    private val lastPeriodPrefs by lazy {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val key by lazy { MicroblogFeedKey(source = "hot", initialSort = initialSort()) }

    private val disposables = CompositeDisposable()
    private var binding: FragmentHotV2Binding? = null
    private var adapter: MicroblogFeedAdapter? = null
    private var currentUi: MicroblogFeedUi? = null

    // Cache mapowanych Entry po id - mutacje (głos/ulubione) trzymają się instancji
    // między emisjami stanu (jak w detalu V2); czyszczony przy przeładowaniu okna.
    private val mappedEntries = mutableMapOf<Long, Entry>()
    private var renderedIds: List<Long> = emptyList()
    private var lastSort: MicroblogFeedSort? = null
    private var votersDialogListener: VotersDialogListener? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHotV2Binding.bind(view).also { this.binding = it }
        val viewModel by viewModels<InjectableViewModel<MicroblogFeedComponent>> {
            viewModelWrapperFactoryKeyed<MicroblogFeedKey, MicroblogFeedComponent>(key = key)
        }
        val getMicroblogFeed = viewModel.dependency.getMicroblogFeed()

        navigation.floatingButton.setOnClickListener { navigatorApi.openAddEntryActivity(requireActivity()) }
        navigation.activityToolbar.overflowIcon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_hot)

        val adapter =
            MicroblogFeedAdapter(
                userManagerApi = userManagerApi,
                settingsPreferencesApi = settingsPreferences,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = this,
            ).also { this.adapter = it }
        binding.recyclerView.prepare()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val ui = currentUi ?: return
                    if (ui.hasMore && layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - LOAD_MORE_THRESHOLD) {
                        ui.loadMoreAction()
                    }
                }
            },
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val shared = getMicroblogFeed().stateIn(this)
                launch { shared.collect { render(it) } }
                launch { shared.map { it.errorDialog }.collectErrorDialog(view.context) }
                launch { shared.map { it.swipeRefresh }.collectSwipeRefresh(binding.swipeRefresh) }
            }
        }
    }

    override fun onDestroyView() {
        disposables.clear()
        binding = null
        adapter = null
        currentUi = null
        super.onDestroyView()
    }

    private fun render(ui: MicroblogFeedUi) {
        val binding = binding ?: return
        val adapter = adapter ?: return
        currentUi = ui
        binding.loadingView.isVisible = ui.isInitialLoading
        navigation.activityToolbar.setTitle(ui.sort.titleRes())

        val mapped =
            ui.entries.map { response ->
                mappedEntries.getOrPut(response.id) { response.filterEntryV3(owmContentFilter) }
            }
        val newIds = mapped.map { it.id }
        // Feed tylko dokleja na końcu: gdy dotychczasowe id są prefiksem nowej listy,
        // wstawiamy ogon; w innym razie (refresh / zmiana trybu) pełne przeładowanie.
        val isAppend = renderedIds.isNotEmpty() && newIds.size >= renderedIds.size && newIds.subList(0, renderedIds.size) == renderedIds
        if (isAppend) {
            adapter.append(mapped.subList(renderedIds.size, newIds.size))
            adapter.setFooterLoading(ui.hasMore)
        } else {
            mappedEntries.keys.retainAll(newIds.toSet())
            adapter.replaceAll(mapped, ui.hasMore)
            // Zmiana trybu (np. 24h -> najnowsze) resetuje listę - przewijamy na górę,
            // inaczej użytkownik zostaje w połowie poprzedniej listy.
            if (lastSort != null && lastSort != ui.sort) {
                binding.recyclerView.post { binding.recyclerView.scrollToPosition(0) }
            }
        }
        lastSort = ui.sort
        renderedIds = newIds
    }

    // ===== menu okresu =====

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater,
    ) {
        inflater.inflate(R.menu.hot_period, menu)
        currentUi?.let { navigation.activityToolbar.setTitle(it.sort.titleRes()) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.refresh) {
            currentUi?.swipeRefresh?.refreshAction?.invoke()
            return true
        }
        val sort =
            when (item.itemId) {
                R.id.period2 -> MicroblogFeedSort.HOT_2
                R.id.period6 -> MicroblogFeedSort.HOT_6
                R.id.period12 -> MicroblogFeedSort.HOT_12
                R.id.period24 -> MicroblogFeedSort.HOT_24
                R.id.active -> MicroblogFeedSort.ACTIVE
                R.id.newest -> MicroblogFeedSort.NEWEST
                else -> return false
            }
        lastPeriodPrefs.edit().putString(KEY_LAST_PERIOD, sort.prefValue()).apply()
        navigation.activityToolbar.setTitle(sort.titleRes())
        currentUi?.selectSortAction?.invoke(sort)
        return true
    }

    // ===== akcje na wpisie (reużycie EntriesInteractor, jak detal V2) =====

    override fun voteEntry(entry: Entry) = entriesInteractor.voteEntry(entry).process(entry)

    override fun unvoteEntry(entry: Entry) = entriesInteractor.unvoteEntry(entry).process(entry)

    override fun markFavorite(entry: Entry) = entriesInteractor.markFavorite(entry).process(entry)

    override fun deleteEntry(entry: Entry) = entriesInteractor.deleteEntry(entry).process(entry)

    override fun observeDiscussion(entry: Entry) = entriesInteractor.observeDiscussion(entry).process(entry)

    override fun voteSurvey(
        entry: Entry,
        index: Int,
    ) = entriesInteractor.voteSurvey(entry, index).process(entry)

    override fun getVoters(entry: Entry) {
        openVotersMenu()
        entriesApi
            .getEntryVoters(entry.id)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe({ votersDialogListener?.invoke(it) }, ::showError)
            .intoComposite(disposables)
    }

    private fun Single<Entry>.process(entry: Entry) {
        subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { adapter?.updateEntry(it) },
                {
                    showError(it)
                    adapter?.updateEntry(entry)
                },
            ).intoComposite(disposables)
    }

    private fun openVotersMenu() {
        val dialog = BottomSheetDialog(requireContext())
        val votersDialogView = DialogVotersBinding.inflate(layoutInflater)
        votersDialogView.votersTextView.isVisible = false
        dialog.setContentView(votersDialogView.root)
        votersDialogListener = createVotersDialogListener(dialog, votersDialogView)
        dialog.show()
    }

    private fun showError(error: Throwable) {
        if (isAdded) requireContext().showExceptionDialog(error)
    }

    private fun initialSort(): MicroblogFeedSort =
        (lastPeriodPrefs.getString(KEY_LAST_PERIOD, null) ?: settingsPreferences.hotEntriesScreen).toSort()

    companion object {
        private const val PREFS_NAME = "mikroblog"
        private const val KEY_LAST_PERIOD = "last_period"
        private const val LOAD_MORE_THRESHOLD = 3

        fun newInstance() = HotFragmentV2()
    }
}

private fun String?.toSort(): MicroblogFeedSort =
    when (this) {
        "24" -> MicroblogFeedSort.HOT_24
        "12" -> MicroblogFeedSort.HOT_12
        "6" -> MicroblogFeedSort.HOT_6
        "2" -> MicroblogFeedSort.HOT_2
        "active" -> MicroblogFeedSort.ACTIVE
        else -> MicroblogFeedSort.NEWEST
    }

private fun MicroblogFeedSort.prefValue(): String =
    when (this) {
        MicroblogFeedSort.HOT_24 -> "24"
        MicroblogFeedSort.HOT_12 -> "12"
        MicroblogFeedSort.HOT_6 -> "6"
        MicroblogFeedSort.HOT_2 -> "2"
        MicroblogFeedSort.ACTIVE -> "active"
        MicroblogFeedSort.NEWEST -> "newest"
    }

private fun MicroblogFeedSort.titleRes(): Int =
    when (this) {
        MicroblogFeedSort.HOT_24 -> R.string.period24
        MicroblogFeedSort.HOT_12 -> R.string.period12
        MicroblogFeedSort.HOT_6 -> R.string.period6
        MicroblogFeedSort.HOT_2 -> R.string.period2
        MicroblogFeedSort.ACTIVE -> R.string.active_entries
        MicroblogFeedSort.NEWEST -> R.string.newest_entries
    }

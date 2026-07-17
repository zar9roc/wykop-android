package io.github.wykopmobilny.ui.modules.mikroblog.entry.v2

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
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
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.databinding.ActivityEntryV2Binding
import io.github.wykopmobilny.databinding.DialogVotersBinding
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsComponent
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsKey
import io.github.wykopmobilny.entries.details.EntryDetailsUi
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.mapper.apiv3.EntryCommentMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.dialogs.VotersDialogListener
import io.github.wykopmobilny.ui.dialogs.createVotersDialogListener
import io.github.wykopmobilny.ui.dialogs.exitConfirmationDialog
import io.github.wykopmobilny.ui.dialogs.showExceptionDialog
import io.github.wykopmobilny.ui.fragments.entries.EntriesInteractor
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentViewListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.ui.widgets.InputToolbar
import io.github.wykopmobilny.ui.widgets.InputToolbarListener
import io.github.wykopmobilny.utils.bindings.collectErrorDialog
import io.github.wykopmobilny.utils.bindings.collectSwipeRefresh
import io.github.wykopmobilny.utils.bindings.bindBackButton
import io.github.wykopmobilny.utils.intoComposite
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.longArgument
import io.github.wykopmobilny.utils.longArgumentNullable
import io.github.wykopmobilny.utils.prepare
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.github.wykopmobilny.utils.viewModelWrapperFactoryKeyed
import io.github.wykopmobilny.utils.InjectableViewModel
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ekran wpisu V2 (port na nowy stack). Domain jest właścicielem danych i
 * dwukierunkowej paginacji (EntryCommentsPager); fragment mapuje odpowiedzi v3
 * na modele legacy i renderuje sprawdzonymi viewholderami starego ekranu,
 * a akcje na treści (głosy, ulubione, ankiety...) idą przez istniejące
 * interaktory - dokładnie ten sam kod, który wykonywał stary prezenter.
 */
internal class EntryDetailsFragment :
    Fragment(R.layout.activity_entry_v2),
    EntryActionListener,
    EntryCommentActionListener,
    EntryCommentViewListener,
    InputToolbarListener {
    @Inject
    lateinit var userManagerApi: UserManagerApi

    @Inject
    lateinit var suggestApi: SuggestApi

    @Inject
    lateinit var entriesApi: EntriesApi

    @Inject
    lateinit var entriesInteractor: EntriesInteractor

    @Inject
    lateinit var entryCommentInteractor: EntryCommentInteractor

    @Inject
    lateinit var schedulers: Schedulers

    @Inject
    lateinit var settingsPreferencesApi: SettingsPreferencesApi

    @Inject
    lateinit var owmContentFilter: OWMContentFilter

    private var entryId by longArgument("entryId")
    private var commentId by longArgumentNullable("commentId")
    private var page by longArgumentNullable("page")

    private val key
        get() =
            EntryDetailsKey(
                entryId = entryId,
                initialCommentId = commentId,
                initialPage = page?.toInt(),
            )

    private val disposables = CompositeDisposable()

    private var binding: ActivityEntryV2Binding? = null
    private var adapter: EntryDetailsAdapterV2? = null
    private var currentUi: EntryDetailsUi? = null
    private var refreshAction: (() -> Unit)? = null
    private var jumpToNewestAction: (() -> Unit)? = null

    // Reconciliation listy: id komentarzy aktualnie w adapterze (po filtrze).
    private var renderedIds: List<Long> = emptyList()
    private var lastEntryResponse: Any? = null
    private val mappedComments = mutableMapOf<Long, EntryComment>()

    private var pendingHighlightCommentId: Long? = null
    private var pendingScrollToBottom = false
    private var inputToolbarConfigured = false

    private var votersDialogListener: VotersDialogListener? = null
    private var cameraPhotoUri: Uri? = null

    private val galleryPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { binding?.inputToolbar?.setPhoto(it) }
        }
    private val cameraCapture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            if (saved) cameraPhotoUri?.let { binding?.inputToolbar?.setPhoto(it) }
        }

    // NewNavigator/WykopLinkHandler wymagają Activity - fragment injectuje się
    // z komponentu aplikacji (wzorzec LinkDetailsFragment), więc budowane ręcznie.
    private val navigator by lazy { NewNavigator(requireActivity(), settingsPreferencesApi) }
    private val linkHandler by lazy { WykopLinkHandler(requireActivity(), navigator) }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel by viewModels<InjectableViewModel<EntryDetailsComponent>> {
            viewModelWrapperFactoryKeyed<EntryDetailsKey, EntryDetailsComponent>(key = key)
        }
        val getEntryDetails = viewModel.dependency.getEntryDetails()
        val binding = ActivityEntryV2Binding.bind(view).also { this.binding = it }
        pendingHighlightCommentId = commentId

        val toolbar = binding.toolbar.root as Toolbar
        toolbar.bindBackButton(activity = activity)
        toolbar.title = getString(R.string.entry)
        toolbar.inflateMenu(R.menu.entry_fragment_menu)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.refresh) {
                refreshAction?.invoke()
                true
            } else {
                false
            }
        }

        val adapter =
            EntryDetailsAdapterV2(
                userManagerApi = userManagerApi,
                settingsPreferencesApi = settingsPreferencesApi,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = this,
                commentActionListener = this,
                commentViewListener = this,
            ).also { this.adapter = it }
        adapter.highlightCommentId = commentId
        binding.recyclerView.prepare()
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    triggerLazyLoads(recyclerView, dy)
                    updateFabState()
                }
            },
        )

        binding.inputToolbar.setup(userManagerApi, suggestApi)
        binding.inputToolbar.inputToolbarListener = this
        binding.inputToolbar.setCustomHint(getString(R.string.reply))
        binding.inputToolbar.hide()

        binding.jumpToNewestFab.setOnClickListener {
            val ui = currentUi ?: return@setOnClickListener
            if (ui.hasNewer) {
                // Ostatniej strony nie ma w oknie - skok przez pager.
                pendingScrollToBottom = true
                ui.lastPage?.let { lastPage ->
                    Toast
                        .makeText(requireContext(), getString(R.string.jump_to_newest_loading, lastPage), Toast.LENGTH_SHORT)
                        .show()
                }
                jumpToNewestAction?.invoke()
            } else {
                // Ostatnia strona załadowana - zwykłe przewinięcie na dół.
                adapter?.let { binding.recyclerView.scrollToPosition(it.itemCount - 1) }
            }
        }

        // Callback przechwytuje wstecz TYLKO gdy jest niezapisana treść (wtedy pytamy
        // o potwierdzenie). Przy pustym polu callback jest wyłączony, więc wstecz
        // obsługuje system - i pokazuje predykcyjny podgląd poprzedniego ekranu.
        val exitConfirmCallback =
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, enabled = false) {
                exitConfirmationDialog(requireContext()) { requireActivity().finish() }?.show()
            }
        binding.inputToolbar.doOnContentChanged {
            exitConfirmCallback.isEnabled = binding.inputToolbar.hasUserEditedContent()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val shared = getEntryDetails().stateIn(this)

                launch { shared.collect { render(it) } }
                launch { shared.map { it.errorDialog }.collectErrorDialog(view.context) }
                launch { shared.map { it.swipeRefresh }.collectSwipeRefresh(binding.swiperefresh) }
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

    // ==================== Rendering ====================

    private fun render(ui: EntryDetailsUi) {
        val binding = binding ?: return
        val adapter = adapter ?: return
        currentUi = ui
        refreshAction = ui.swipeRefresh.refreshAction
        jumpToNewestAction = ui.jumpToNewestAction

        binding.loadingView.isVisible = ui.isInitialLoading
        binding.loadingOlderView.isVisible = ui.isLoadingOlder
        updateFabState()

        val entryResponse = ui.entry ?: return
        val mappedEntry =
            if (lastEntryResponse !== entryResponse || adapter.entry == null) {
                entryResponse.filterEntryV3(owmContentFilter).also { it.comments.clear() }
            } else {
                null
            }

        val mapped =
            ui.comments
                .map { response ->
                    mappedComments.getOrPut(response.id) {
                        EntryCommentMapperV3.map(response, owmContentFilter, entryId = entryId)
                    }
                }.filterNot { settingsPreferencesApi.hideBlacklistedViews && it.isBlocked && it.deletedReason == null }
        val newIds = mapped.map { it.id }
        val oldIds = renderedIds

        when {
            adapter.entry == null || oldIds.isEmpty() || newIds.isEmpty() || !isWindowContinuation(oldIds, newIds) -> {
                adapter.replaceAll(
                    newEntry = mappedEntry ?: adapter.entry,
                    newComments = mapped,
                    footerLoading = ui.hasNewer,
                    // Wpis nad komentarzami dopiero, gdy okno sięga strony 1 -
                    // wcześniej scroll w górę dociąga starsze strony.
                    headerVisible = !ui.hasOlder,
                )
            }

            else -> {
                mappedEntry?.let(adapter::updateEntry)
                if (newIds.size > oldIds.size) {
                    val prependCount = newIds.indexOf(oldIds.first())
                    if (prependCount > 0) {
                        adapter.prepend(mapped.subList(0, prependCount))
                    }
                    val appendCount = newIds.size - prependCount - oldIds.size
                    if (appendCount > 0) {
                        adapter.append(mapped.subList(newIds.size - appendCount, newIds.size))
                    }
                }
                adapter.setFooterLoading(ui.hasNewer)
                // Po prependzie, żeby header wskoczył nad świeżo dodaną stronę 1.
                adapter.setHeaderVisible(!ui.hasOlder)
            }
        }
        renderedIds = newIds

        if (mappedEntry != null) {
            lastEntryResponse = entryResponse
            configureInputToolbar(mappedEntry)
        }
        handlePendingScrolls(binding, adapter, ui)
    }

    // Nowa lista jest kontynuacją okna, gdy stare id-ki występują w niej w całości
    // jako spójny blok (prepend/append) - w innym razie pełny reset adaptera.
    private fun isWindowContinuation(
        oldIds: List<Long>,
        newIds: List<Long>,
    ): Boolean {
        val start = newIds.indexOf(oldIds.first())
        if (start < 0 || start + oldIds.size > newIds.size) return false
        return newIds.subList(start, start + oldIds.size) == oldIds
    }

    private fun handlePendingScrolls(
        binding: ActivityEntryV2Binding,
        adapter: EntryDetailsAdapterV2,
        ui: EntryDetailsUi,
    ) {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return
        pendingHighlightCommentId?.let { highlightId ->
            adapter.positionOfComment(highlightId)?.let { position ->
                pendingHighlightCommentId = null
                binding.recyclerView.post { layoutManager.scrollToPositionWithOffset(position, 0) }
            }
        }
        if (pendingScrollToBottom && !ui.isInitialLoading && adapter.itemCount > 0) {
            pendingScrollToBottom = false
            binding.recyclerView.post { binding.recyclerView.scrollToPosition(adapter.itemCount - 1) }
        }
        // Gdy załadowane okno nie wypełnia ekranu, scroll listener się nie odpali -
        // dociągamy kolejną stronę, aż pojawi się przewijanie albo skończą strony.
        if (ui.hasNewer && !ui.isLoadingNewer) {
            binding.recyclerView.post {
                val recycler = this.binding?.recyclerView ?: return@post
                if (!recycler.canScrollVertically(1) && !recycler.canScrollVertically(-1)) {
                    currentUi?.loadNewerAction?.invoke()
                }
            }
        }
        // Bufor w górę: gdy viewport jest przy szczycie okna (np. kotwica na
        // początku strony), preładowujemy starszą stronę - inaczej użytkownik
        // "dobija" do góry i scroll listener nie ma już eventów dy<0.
        if (ui.hasOlder && !ui.isLoadingOlder) {
            binding.recyclerView.post {
                val recycler = this.binding?.recyclerView ?: return@post
                val manager = recycler.layoutManager as? LinearLayoutManager ?: return@post
                if (manager.findFirstVisibleItemPosition() <= LOAD_OLDER_THRESHOLD) {
                    currentUi?.loadOlderAction?.invoke()
                }
            }
        }
    }

    /**
     * Trzy stany FAB-a: podwójny szewron (skok - ostatnia strona poza oknem),
     * pojedynczy szewron (przewiń na dół - ostatnia strona w oknie, ale nie jesteśmy
     * na dole), ukryty (dół listy albo brak paginacji).
     */
    private fun updateFabState() {
        val binding = binding ?: return
        val ui = currentUi ?: return
        val hasPagination = (ui.lastPage ?: 0) > 1
        val atBottom = !binding.recyclerView.canScrollVertically(1)
        when {
            !hasPagination || ui.isInitialLoading -> binding.jumpToNewestFab.isVisible = false

            ui.hasNewer -> {
                binding.jumpToNewestFab.setImageResource(R.drawable.ic_jump_to_newest)
                binding.jumpToNewestFab.isVisible = true
            }

            atBottom -> binding.jumpToNewestFab.isVisible = false

            else -> {
                binding.jumpToNewestFab.setImageResource(R.drawable.ic_scroll_to_bottom)
                binding.jumpToNewestFab.isVisible = true
            }
        }
    }

    private fun triggerLazyLoads(
        recyclerView: RecyclerView,
        dy: Int,
    ) {
        val ui = currentUi ?: return
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        if (dy > 0 && ui.hasNewer && layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - LOAD_MORE_THRESHOLD) {
            ui.loadNewerAction()
        }
        if (dy < 0 && ui.hasOlder && layoutManager.findFirstVisibleItemPosition() <= LOAD_OLDER_THRESHOLD) {
            ui.loadOlderAction()
        }
    }

    private fun configureInputToolbar(entry: Entry) {
        val binding = binding ?: return
        if (!userManagerApi.isUserAuthorized()) {
            binding.inputToolbar.hide()
            return
        }
        if (!inputToolbarConfigured) {
            inputToolbarConfigured = true
            binding.inputToolbar.setDefaultAddressant(entry.author.nick)
        }
        binding.inputToolbar.setIfIsCommentingPossible(entry.isCommentingPossible)
        binding.inputToolbar.show()
    }

    /** Wołane przez EntryActivityV2 po powrocie z edycji wpisu/komentarza. */
    fun onContentEdited() {
        mappedComments.clear()
        lastEntryResponse = null
        refreshAction?.invoke()
    }

    // ==================== Akcje na wpisie (EntryActionListener) ====================

    override fun voteEntry(entry: Entry) = entriesInteractor.voteEntry(entry).processEntrySingle(entry)

    override fun unvoteEntry(entry: Entry) = entriesInteractor.unvoteEntry(entry).processEntrySingle(entry)

    override fun markFavorite(entry: Entry) = entriesInteractor.markFavorite(entry).processEntrySingle(entry)

    override fun deleteEntry(entry: Entry) = entriesInteractor.deleteEntry(entry).processEntrySingle(entry)

    override fun voteSurvey(
        entry: Entry,
        index: Int,
    ) = entriesInteractor.voteSurvey(entry, index).processEntrySingle(entry)

    override fun getVoters(entry: Entry) {
        openVotersMenu()
        entriesApi
            .getEntryVoters(entry.id)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { votersDialogListener?.invoke(it) },
                ::showError,
            ).intoComposite(disposables)
    }

    // ==================== Akcje na komentarzach ====================

    override fun voteComment(comment: EntryComment) = entryCommentInteractor.voteComment(comment).processEntryCommentSingle(comment)

    override fun unvoteComment(comment: EntryComment) = entryCommentInteractor.unvoteComment(comment).processEntryCommentSingle(comment)

    override fun deleteComment(comment: EntryComment) = entryCommentInteractor.deleteComment(comment).processEntryCommentSingle(comment)

    override fun getVoters(comment: EntryComment) {
        openVotersMenu()
        entriesApi
            .getEntryCommentVoters(comment.entryId, comment.id)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { votersDialogListener?.invoke(it) },
                ::showError,
            ).intoComposite(disposables)
    }

    override fun addReply(author: Author) {
        binding?.inputToolbar?.addAddressant(author.nick)
    }

    override fun quoteComment(comment: EntryComment) {
        binding?.inputToolbar?.addQuoteText(comment.body, comment.author.nick)
    }

    // ==================== Wysyłanie komentarza (InputToolbarListener) ====================

    override fun sendPhoto(
        photo: String?,
        body: String,
        containsAdultContent: Boolean,
    ) {
        entriesApi
            .addEntryComment(body, entryId, photo, containsAdultContent)
            .handleCommentSent()
    }

    override fun sendPhoto(
        photo: WykopImageFile,
        body: String,
        containsAdultContent: Boolean,
    ) {
        entriesApi
            .addEntryComment(body, entryId, photo, containsAdultContent)
            .handleCommentSent()
    }

    private fun <T : Any> Single<T>.handleCommentSent() {
        subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    binding?.inputToolbar?.showProgress(false)
                    binding?.inputToolbar?.resetState()
                    // Nowy komentarz ląduje na ostatniej stronie - przy paginacji
                    // skaczemy do najnowszych, bez paginacji wystarczy refresh.
                    pendingScrollToBottom = true
                    jumpToNewestAction?.invoke() ?: refreshAction?.invoke()
                },
                {
                    binding?.inputToolbar?.showProgress(false)
                    showError(it)
                },
            ).intoComposite(disposables)
    }

    override fun openGalleryImageChooser() {
        galleryPicker.launch("image/*")
    }

    override fun openCamera(uri: Uri) {
        cameraPhotoUri = uri
        cameraCapture.launch(uri)
    }

    // ==================== Pomocnicze ====================

    private fun openVotersMenu() {
        val dialog = BottomSheetDialog(requireContext())
        val votersDialogView = DialogVotersBinding.inflate(layoutInflater)
        votersDialogView.votersTextView.isVisible = false
        dialog.setContentView(votersDialogView.root)
        votersDialogListener = createVotersDialogListener(dialog, votersDialogView)
        dialog.show()
    }

    private fun showError(error: Throwable) {
        if (isAdded) {
            requireContext().showExceptionDialog(error)
        }
    }

    private fun Single<Entry>.processEntrySingle(entry: Entry) {
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

    private fun Single<EntryComment>.processEntryCommentSingle(comment: EntryComment) {
        subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    mappedComments[it.id] = it
                    adapter?.updateComment(it)
                },
                {
                    showError(it)
                    adapter?.updateComment(comment)
                },
            ).intoComposite(disposables)
    }

    companion object {
        private const val LOAD_MORE_THRESHOLD = 3
        private const val LOAD_OLDER_THRESHOLD = 2

        fun newInstance(
            entryId: Long,
            commentId: Long?,
            page: Int?,
        ) = EntryDetailsFragment().apply {
            this.entryId = entryId
            this.commentId = commentId
            this.page = page?.toLong()
        }
    }
}

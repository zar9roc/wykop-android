package io.github.wykopmobilny.ui.modules.mikroblog.threadcontext

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.AndroidSupportInjection
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.PhotoSource
import io.github.wykopmobilny.ui.widgets.MAX_MICROBLOG_PHOTOS
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.databinding.FragmentThreadContextBinding
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.ThreadChunk
import io.github.wykopmobilny.models.dataclass.ThreadContext
import io.github.wykopmobilny.models.dataclass.ThreadDescendant
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.dialogs.showExceptionDialog
import io.github.wykopmobilny.ui.fragments.entries.EntriesInteractor
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentViewListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.ui.widgets.InputToolbarListener
import io.github.wykopmobilny.utils.bindings.bindBackButton
import io.github.wykopmobilny.utils.intoComposite
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.longArgument
import io.github.wykopmobilny.utils.prepareNoDivider
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

/**
 * Ekran "kontekst watku": wpis, kolejni rodzice i komentarz, z ktorego weszlismy
 * (plasko, ze strzalkami miedzy pozycjami), a pod nim cale poddrzewo odpowiedzi tego
 * komentarza - z wcieciem wedlug glebokosci. Otwiera sie ustawiony na komentarzu
 * docelowym, a pasek na dole odpowiada ZAGNIEZDZONO w tym watku.
 *
 * Sciezka idzie z /v3/entries-threads/.../ancestors - plaska lista komentarzy wpisu
 * nie niesie relacji rodzic-dziecko, wiec bez tego ekranu kontekstu nie da sie odtworzyc.
 *
 * Poddrzewo NIE jest ladowane zachlannie: dwa pierwsze zapytania oddaja wpis, sciezke
 * i komentarz docelowy (z ekspansja), a brakujace galezie dociaga [ThreadContextPager]
 * w miare scrollowania - tak jak na ekranie komentarzy znaleziska.
 */
internal class ThreadContextFragment :
    Fragment(R.layout.fragment_thread_context),
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

    private var entryId by longArgument("entryId")
    private var commentId by longArgument("commentId")

    private var binding: FragmentThreadContextBinding? = null
    private var adapter: ThreadContextAdapter? = null
    private var threadContext: ThreadContext? = null
    private var pager: ThreadContextPager? = null

    /**
     * Komentarz, na ktorym ma sie ustawic lista, a ktorego jeszcze nie dociagnelismy
     * (swiezo wyslana odpowiedz w niepobranej galezi). Dopoki nie jest null, doladowania
     * ida priorytetowo w [priorityParents] - odpowiednik `key.initialCommentId`
     * w LinkCommentsPager.
     */
    private var pendingFocusCommentId: Long? = null
    private var priorityParents = emptyList<Long>()
    private var priorityRequests = 0

    /**
     * Komentarz, pod ktory poleci odpowiedz. Domyslnie docelowy, ale po tapnieciu
     * "Odpowiedz"/"Cytuj" pod konkretnym potomkiem - ten potomek.
     */
    private var replyTargetCommentId: Long = 0

    /** Komentarz aktualnie podswietlony na liscie (docelowy albo swiezo wyslana odpowiedz). */
    private var highlightedCommentId: Long = 0
    private val disposables = CompositeDisposable()
    private var cameraPhotoUri: Uri? = null

    private val galleryPicker =
        // Wiele zdjec naraz (galeria mikrobloga) - nadmiar ponad limit jest pomijany.
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            val inputToolbar = binding?.inputToolbar ?: return@registerForActivityResult
            uris.take(inputToolbar.remainingPhotos).forEach(inputToolbar::addPhoto)
        }
    private val cameraCapture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            if (saved) cameraPhotoUri?.let { binding?.inputToolbar?.addPhoto(it) }
        }

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
        val binding = FragmentThreadContextBinding.bind(view).also { this.binding = it }

        (binding.toolbar.root as Toolbar).apply {
            bindBackButton(activity = activity)
            title = getString(R.string.thread_context_title)
        }

        val adapter =
            ThreadContextAdapter(
                userManagerApi = userManagerApi,
                settingsPreferencesApi = settingsPreferencesApi,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = this,
                commentActionListener = this,
                commentViewListener = this,
            ).also { this.adapter = it }

        // Separatory rysuje dekorator na podstawie tagow viewholderow - tu granice
        // wyznaczaja strzalki, wiec linie tylko by szumialy.
        binding.recyclerView.prepareNoDivider()
        binding.recyclerView.adapter = adapter

        pager =
            ThreadContextPager(
                entriesApi = entriesApi,
                schedulers = schedulers,
                entryId = entryId,
                disposables = disposables,
                onChunk = ::appendChunk,
                onLoadingChanged = { isLoading -> this.adapter?.setLoadingMore(isLoading) },
            )
        // Leniwe ladowanie: galaz dociagamy, gdy jej komentarz wejdzie w pole widzenia
        // albo gdy uzytkownik doscrolluje do dolu (dedup i priorytet obsluguje pager).
        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    triggerLazyLoads()
                }
            },
        )

        binding.inputToolbar.setup(userManagerApi, suggestApi)
        binding.inputToolbar.inputToolbarListener = this
        binding.inputToolbar.maxPhotos = MAX_MICROBLOG_PHOTOS
        binding.inputToolbar.setCustomHint(getString(R.string.reply))
        binding.inputToolbar.hide()

        loadContext()
    }

    override fun onDestroyView() {
        disposables.clear()
        binding = null
        adapter = null
        pager = null
        super.onDestroyView()
    }

    /**
     * Dwa zapytania i render - wpis, sciezka w gore i komentarz docelowy pokazuja sie
     * od razu, a poddrzewo dochodzi pozniej, galaz po galezi, przy scrollowaniu.
     *
     * [focusCommentId] - komentarz, na ktorym ma sie ustawic lista i ktory ma byc
     * podswietlony; domyslnie docelowy, po wyslaniu odpowiedzi - nowy komentarz.
     * [replyParentId] - rodzic swiezo wyslanej odpowiedzi; jego galaz dociagamy
     * priorytetowo, bo inaczej nowego komentarza moze na ekranie jeszcze nie byc.
     */
    /**
     * Wolane przez ThreadContextActivity po powrocie z edycji wpisu/komentarza - watek
     * pobierany od nowa (jak po wyslaniu odpowiedzi), lista zostaje na tym samym komentarzu.
     */
    fun onContentEdited() = loadContext(focusCommentId = highlightedCommentId)

    private fun loadContext(
        focusCommentId: Long = commentId,
        replyParentId: Long? = null,
    ) {
        binding?.loadingView?.isVisible = true
        entriesApi
            .getThreadContext(entryId = entryId, commentId = commentId)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { context ->
                    binding?.loadingView?.isVisible = false
                    threadContext = context
                    showRows(focusCommentId)
                    // Reset uniewaznia doladowania w locie z poprzedniego przebiegu.
                    pager?.reset(context)
                    preparePriorityFocus(focusCommentId, replyParentId)
                    // Po ulozeniu listy - inaczej layout manager nie zna jeszcze widocznych pozycji.
                    binding?.recyclerView?.post { triggerLazyLoads() }
                },
                { error ->
                    binding?.loadingView?.isVisible = false
                    showError(error)
                },
            ).intoComposite(disposables)
    }

    /**
     * Ustala, czy po wyslaniu odpowiedzi trzeba czegos dociagnac, zeby dojsc do nowego
     * komentarza, i ktore galezie maja wtedy pierwszenstwo.
     */
    private fun preparePriorityFocus(
        focusCommentId: Long,
        replyParentId: Long?,
    ) {
        pendingFocusCommentId = null
        priorityParents = emptyList()
        priorityRequests = 0
        if (focusCommentId == commentId || replyParentId == null) return
        if (adapter?.positionOf(focusCommentId) != RecyclerView.NO_POSITION) return
        pendingFocusCommentId = focusCommentId
        // Z poziomu 7 watek sie nie poglebia - API dokleja odpowiedz jako rodzenstwo celu,
        // wiec drugim kandydatem jest galaz rodzica tego komentarza.
        priorityParents = listOfNotNull(replyParentId, parentIdOf(replyParentId))
    }

    /**
     * Rodzic komentarza z poddrzewa: najblizszy wczesniejszy wiersz o mniejszej glebokosci.
     * Dla bezposredniej odpowiedzi celu - sam komentarz docelowy.
     */
    private fun parentIdOf(commentId: Long): Long? {
        val descendants = threadContext?.descendants ?: return null
        val index = descendants.indexOfFirst { it.comment.id == commentId }
        if (index < 0) return null
        val depth = descendants[index].depth
        if (depth <= 1) return this.commentId
        for (position in index - 1 downTo 0) {
            if (descendants[position].depth < depth) return descendants[position].comment.id
        }
        return this.commentId
    }

    /**
     * Decyduje, co dociagnac: najpierw galaz z priorytetem (droga do swiezo wyslanej
     * odpowiedzi), potem galezie komentarzy widocznych na ekranie, a na koncu - gdy
     * uzytkownik jest przy dole listy - pierwsza galaz z kolejki.
     */
    private fun triggerLazyLoads() {
        val adapter = adapter ?: return
        val pager = pager ?: return
        if (loadPriorityBranch()) return
        val layoutManager = binding?.recyclerView?.layoutManager as? LinearLayoutManager ?: return
        if (adapter.itemCount == 0) return
        val last = layoutManager.findLastVisibleItemPosition()
        if (last == RecyclerView.NO_POSITION) return
        val first = layoutManager.findFirstVisibleItemPosition().coerceAtLeast(0)
        for (position in first..last) {
            val id = adapter.commentIdAt(position) ?: continue
            if (pager.requestBranch(id)) return
        }
        if (last >= adapter.itemCount - LOAD_MORE_THRESHOLD) {
            pager.loadNext()
        }
    }

    /** Dociaga galaz prowadzaca do komentarza, na ktorym ma sie ustawic lista. */
    private fun loadPriorityBranch(): Boolean {
        val pager = pager ?: return false
        if (pendingFocusCommentId == null) return false
        if (priorityRequests >= MAX_PRIORITY_REQUESTS) {
            pendingFocusCommentId = null
            return false
        }
        priorityParents.forEach { parentId ->
            if (pager.requestBranch(parentId)) {
                priorityRequests++
                return true
            }
        }
        // Nie ma juz czego dociagac w tych galeziach - przestajemy szukac komentarza.
        if (priorityParents.none { pager.hasPending(it) }) {
            pendingFocusCommentId = null
        }
        return false
    }

    /**
     * Wkleja swiezo dociagnieta porcje poddrzewa na koniec poddrzewa jej rodzica -
     * kolejnosc preorder (dziecko, potem cale jego poddrzewo) zostaje zachowana, a nowe
     * odpowiedzi sa chronologicznie za tymi, ktore juz mamy (kursor `oldest` po id).
     */
    private fun appendChunk(chunk: ThreadChunk) {
        val context = threadContext ?: return
        val adapter = adapter ?: return
        val fresh = withoutKnownComments(chunk.descendants, context)
        if (fresh.isNotEmpty()) {
            val merged = context.descendants.toMutableList()
            val insertAt = subtreeEnd(merged, chunk.parentId)
            merged.addAll(insertAt, fresh)
            val updated = context.copy(descendants = merged)
            threadContext = updated
            adapter.insertDescendants(updated, descendantIndex = insertAt, count = fresh.size)
        }
        val focusId = pendingFocusCommentId
        if (focusId != null) {
            val position = adapter.positionOf(focusId)
            if (position != RecyclerView.NO_POSITION) {
                pendingFocusCommentId = null
                priorityParents = emptyList()
                scrollTo(position)
            }
        }
        triggerLazyLoads()
    }

    /**
     * Dedup po id: komentarz, ktory juz jest na ekranie, odpada razem ze swoim poddrzewem
     * z tej porcji (jego dzieci przyszly wtedy pod juz istniejacym wierszem).
     */
    private fun withoutKnownComments(
        descendants: List<ThreadDescendant>,
        context: ThreadContext,
    ): List<ThreadDescendant> {
        val known = mutableSetOf<Long>()
        context.path.forEach { row -> (row as? EntryListRow.CommentRow)?.let { known += it.comment.id } }
        context.descendants.forEach { known += it.comment.id }
        val result = mutableListOf<ThreadDescendant>()
        var skippedDepth: Int? = null
        descendants.forEach { descendant ->
            val skipped = skippedDepth
            if (skipped != null && descendant.depth > skipped) return@forEach
            skippedDepth = null
            if (!known.add(descendant.comment.id)) {
                skippedDepth = descendant.depth
                return@forEach
            }
            result += descendant
        }
        return result
    }

    /** Koniec poddrzewa danego rodzica na plaskiej liscie - tam dochodzi kolejna strona. */
    private fun subtreeEnd(
        descendants: List<ThreadDescendant>,
        parentId: Long,
    ): Int {
        // Cala lista potomkow to poddrzewo komentarza docelowego.
        if (parentId == commentId) return descendants.size
        val index = descendants.indexOfFirst { it.comment.id == parentId }
        if (index < 0) return descendants.size
        val depth = descendants[index].depth
        var end = index + 1
        while (end < descendants.size && descendants[end].depth > depth) {
            end++
        }
        return end
    }

    private fun showRows(focusCommentId: Long = commentId) {
        val adapter = adapter ?: return
        val context = threadContext ?: return
        highlightedCommentId = focusCommentId
        adapter.submit(context, highlightCommentId = focusCommentId)
        // Komentarz docelowy nie jest juz ostatni na liscie (ma pod soba poddrzewo),
        // wiec ustawiamy go przy gornej krawedzi - odpowiedzi widac od razu pod nim.
        val position = adapter.positionOf(focusCommentId)
        if (position != RecyclerView.NO_POSITION) {
            scrollTo(position)
        }
        configureInputToolbar()
    }

    private fun scrollTo(position: Int) {
        val layoutManager = binding?.recyclerView?.layoutManager as? LinearLayoutManager
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(position, 0)
        } else {
            binding?.recyclerView?.scrollToPosition(position)
        }
    }

    /** Pasek odpowiedzi z domyslnym adresatem = autor komentarza, w ktory weszlismy. */
    private fun configureInputToolbar() {
        val binding = binding ?: return
        val target = threadContext?.targetComment ?: return
        if (userManagerApi.getUserCredentials() == null) {
            binding.inputToolbar.hide()
            return
        }
        binding.inputToolbar.setDefaultAddressant(target.author.nick)
        binding.inputToolbar.show()
        resetReplyTarget()
    }

    /** Cel odpowiedzi wraca na komentarz docelowy - pasek kontekstu znika. */
    private fun resetReplyTarget() {
        replyTargetCommentId = commentId
        binding?.inputToolbar?.setReplyContext(author = null)
    }

    /** Zapamietuje, pod ktory komentarz ma trafic kolejna odpowiedz. */
    private fun setReplyTarget(comment: EntryComment) {
        replyTargetCommentId = comment.id
        val target = threadContext?.targetComment
        if (comment.id == target?.id) {
            binding?.inputToolbar?.setReplyContext(author = null)
        } else {
            binding?.inputToolbar?.setReplyContext(author = comment.author.nick, onClear = ::resetReplyTarget)
        }
    }

    /**
     * Id komentarza-rodzica dla POST-a. Watek nie schodzi ponizej poziomu 7, wiec
     * odpowiedz na komentarz z tego poziomu podwieszamy pod niego samego - API
     * doklei ja jako kolejny wpis poziomu 7, zamiast odrzucic zadanie.
     */
    private fun currentReplyParentId(): Long = replyTargetCommentId.takeIf { it > 0 } ?: commentId

    // ==================== Akcje na wpisie ====================

    override fun voteEntry(entry: Entry) = entriesInteractor.voteEntry(entry).replaceEntry()

    override fun unvoteEntry(entry: Entry) = entriesInteractor.unvoteEntry(entry).replaceEntry()

    override fun markFavorite(entry: Entry) = entriesInteractor.markFavorite(entry).replaceEntry()

    // Akcje z wlasnym UI (potwierdzenia, listy) zyja na ekranie wpisu - tam kierujemy.
    override fun deleteEntry(entry: Entry) = navigator.openEntryDetailsActivity(entry.id, isRevealed = false)

    override fun observeDiscussion(entry: Entry) = navigator.openEntryDetailsActivity(entry.id, isRevealed = false)

    override fun voteSurvey(
        entry: Entry,
        index: Int,
    ) = navigator.openEntryDetailsActivity(entry.id, isRevealed = false)

    override fun getVoters(entry: Entry) = navigator.openEntryDetailsActivity(entry.id, isRevealed = false)

    // ==================== Akcje na komentarzach ====================

    override fun voteComment(comment: EntryComment) = entryCommentInteractor.voteComment(comment).replaceComment()

    override fun unvoteComment(comment: EntryComment) = entryCommentInteractor.unvoteComment(comment).replaceComment()

    override fun deleteComment(comment: EntryComment) = navigator.openEntryDetailsActivity(comment.entryId, comment.id)

    override fun getVoters(comment: EntryComment) = navigator.openEntryDetailsActivity(comment.entryId, comment.id)

    // ==================== Pasek odpowiedzi ====================

    override fun addReply(comment: EntryComment) {
        setReplyTarget(comment)
        binding?.inputToolbar?.addAddressant(comment.author.nick)
    }

    override fun addReplyToAuthor(author: Author) {
        binding?.inputToolbar?.addAddressant(author.nick)
    }

    override fun quoteComment(comment: EntryComment) {
        setReplyTarget(comment)
        binding?.inputToolbar?.addQuoteText(comment.body, comment.author.nick)
    }

    override fun sendPhotos(
        photos: List<PhotoSource>,
        body: String,
        containsAdultContent: Boolean,
        embedUrl: String?,
    ) {
        entriesApi
            .addThreadReply(body, entryId, currentReplyParentId(), photos, containsAdultContent, embedUrl)
            .handleReplySent()
    }

    override fun openGalleryImageChooser() {
        galleryPicker.launch("image/*")
    }

    override fun openCamera(uri: Uri) {
        cameraPhotoUri = uri
        cameraCapture.launch(uri)
    }

    // ==================== Pomocnicze ====================

    private fun Single<Long>.handleReplySent() {
        val replyParentId = currentReplyParentId()
        subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { newCommentId ->
                    binding?.inputToolbar?.showProgress(false)
                    binding?.inputToolbar?.resetState()
                    resetReplyTarget()
                    // Odpowiedz wisi w poddrzewie watku - ekran wpisu odswiezy sie
                    // po powrocie (RESULT_OK).
                    activity?.setResult(android.app.Activity.RESULT_OK)
                    // Przeladowanie zaczyna od dwoch zapytan, wiec nowej odpowiedzi moze
                    // w pierwszej partii nie byc - galaz jej rodzica idzie wtedy priorytetowo,
                    // a po dociagnieciu ustawiamy sie na niej i podswietlamy.
                    loadContext(focusCommentId = newCommentId, replyParentId = replyParentId)
                },
                { error ->
                    binding?.inputToolbar?.showProgress(false)
                    showError(error)
                },
            ).intoComposite(disposables)
    }

    private fun Single<Entry>.replaceEntry() {
        subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { updated ->
                    replaceRow { row ->
                        (row as? EntryListRow.EntryRow)?.takeIf { it.entry.id == updated.id }?.let { EntryListRow.EntryRow(updated) }
                    }
                },
                ::showError,
            ).intoComposite(disposables)
    }

    private fun Single<EntryComment>.replaceComment() {
        subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { updated ->
                    replaceRow { row ->
                        (row as? EntryListRow.CommentRow)?.takeIf { it.comment.id == updated.id }?.let { EntryListRow.CommentRow(updated) }
                    }
                },
                ::showError,
            ).intoComposite(disposables)
    }

    /**
     * Podmienia jeden wiersz (sciezka w gore albo poddrzewo) po glosowaniu, bez
     * ponownego dociagania calego watku i bez przewijania listy.
     */
    private fun replaceRow(replacement: (EntryListRow) -> EntryListRow?) {
        val context = threadContext ?: return
        var changed = false
        val path =
            context.path.map { row ->
                val replaced = replacement(row)
                if (replaced == null) {
                    row
                } else {
                    changed = true
                    replaced
                }
            }
        val descendants =
            context.descendants.map { descendant ->
                val replaced = replacement(descendant.row) as? EntryListRow.CommentRow
                if (replaced == null) {
                    descendant
                } else {
                    changed = true
                    descendant.copy(row = replaced)
                }
            }
        if (!changed) return
        val updated = context.copy(path = path, descendants = descendants)
        threadContext = updated
        adapter?.submit(updated, highlightCommentId = highlightedCommentId)
    }

    private fun showError(error: Throwable) {
        if (isAdded) {
            requireContext().showExceptionDialog(error)
        }
    }

    companion object {
        /** Ile pozycji przed koncem listy zaczac dociagac kolejna galaz. */
        private const val LOAD_MORE_THRESHOLD = 3

        /**
         * Gorny limit stron dociaganych "same z siebie" w pogoni za swiezo wyslana
         * odpowiedzia - bez niego dlugi watek mogl by sie dociagac w calosci bez udzialu
         * uzytkownika. Po wyczerpaniu po prostu przestajemy szukac; komentarz dojdzie
         * normalnie, przy scrollowaniu.
         */
        private const val MAX_PRIORITY_REQUESTS = 20

        fun newInstance(
            entryId: Long,
            commentId: Long,
        ) = ThreadContextFragment().apply {
            this.entryId = entryId
            this.commentId = commentId
        }
    }
}

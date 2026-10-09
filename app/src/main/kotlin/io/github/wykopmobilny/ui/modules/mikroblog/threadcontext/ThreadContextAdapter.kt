package io.github.wykopmobilny.ui.modules.mikroblog.threadcontext

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.R
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.ThreadContext
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.constructEntryOrCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.BlockedViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentViewListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.layoutInflater

/** Wiersz ekranu kontekstu: tresc (wpis/komentarz) albo strzalka miedzy nimi. */
internal sealed interface ThreadContextListItem {
    data class Content(
        val row: EntryListRow,
        /** Glebokosc w poddrzewie pod komentarzem docelowym; 0 = sciezka w gore. */
        val depth: Int = 0,
    ) : ThreadContextListItem

    data object Arrow : ThreadContextListItem

    /** Wskaznik na dole listy - cos sie wlasnie dociaga w tle. */
    data object Progress : ThreadContextListItem
}

/**
 * Lista: wpis / strzalka / kolejni rodzice / komentarz docelowy, a pod nim cale
 * poddrzewo odpowiedzi. Sciezke w gore rozdzielaja strzalki, a hierarchie poddrzewa
 * pokazuje wciecie wiersza (patrz [INDENT_DP]).
 *
 * Tresc renderuja te same viewholdery co reszta aplikacji, wiec dziala tu glosowanie,
 * menu i osadzone media; strzalka to wlasny, trywialny typ wiersza.
 */
internal class ThreadContextAdapter(
    private val userManagerApi: io.github.wykopmobilny.utils.usermanager.UserManagerApi,
    settingsPreferencesApi: SettingsPreferencesApi,
    private val navigator: NewNavigator,
    private val linkHandler: io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler,
    private val entryActionListener: EntryActionListener,
    private val commentActionListener: EntryCommentActionListener,
    private val commentViewListener: EntryCommentViewListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
    private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
    private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
    private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
    private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }

    private val items = mutableListOf<ThreadContextListItem>()

    /** Komentarz z podswietlonym tlem (domyslnie docelowy, po wyslaniu - nowa odpowiedz). */
    private var highlightCommentId: Long = 0

    /** Czy na dole listy wisi wskaznik dociagania kolejnej galezi. */
    private var isLoadingMore = false

    /** Ile pozycji zajmuje sciezka w gore (tresc + strzalki) - przed poddrzewem. */
    private var pathItemCount = 0

    fun submit(
        context: ThreadContext,
        highlightCommentId: Long = context.targetComment?.id ?: 0,
    ) {
        this.highlightCommentId = highlightCommentId
        rebuild(context)
        @Suppress("NotifyDataSetChanged") // Pelne przeladowanie kontekstu watku.
        notifyDataSetChanged()
    }

    /**
     * Dokleja [count] swiezo dociagnietych potomkow zaczynajacych sie na pozycji
     * [descendantIndex] listy potomkow. Osobno od [submit], bo przy leniwym ladowaniu
     * lista rosnie co chwile - pelne odswiezenie gubiloby pozycje scrolla i migalo.
     */
    fun insertDescendants(
        context: ThreadContext,
        descendantIndex: Int,
        count: Int,
    ) {
        if (count <= 0) return
        rebuild(context)
        notifyItemRangeInserted(pathItemCount + descendantIndex, count)
    }

    /** Pokazuje/chowa wskaznik dociagania na dole listy. */
    fun setLoadingMore(visible: Boolean) {
        if (isLoadingMore == visible) return
        isLoadingMore = visible
        if (visible) {
            items += ThreadContextListItem.Progress
            notifyItemInserted(items.size - 1)
        } else if (items.lastOrNull() is ThreadContextListItem.Progress) {
            val position = items.size - 1
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun rebuild(context: ThreadContext) {
        items.clear()
        // Sciezka w gore - strzalka miedzy kazda para pozycji.
        context.path.forEachIndexed { index, row ->
            if (index > 0) {
                items += ThreadContextListItem.Arrow
            }
            items += ThreadContextListItem.Content(row)
        }
        pathItemCount = items.size
        // Poddrzewo - bez strzalek, hierarchie niesie wciecie.
        context.descendants.forEach { descendant ->
            items += ThreadContextListItem.Content(descendant.row, depth = descendant.depth)
        }
        if (isLoadingMore) {
            items += ThreadContextListItem.Progress
        }
    }

    /** Pozycja komentarza na liscie albo [RecyclerView.NO_POSITION], gdy go tu nie ma. */
    fun positionOf(commentId: Long): Int =
        items.indexOfFirst { item ->
            item is ThreadContextListItem.Content &&
                item.row is EntryListRow.CommentRow &&
                item.row.comment.id == commentId
        }

    /** Id komentarza na danej pozycji albo null, gdy to wpis, strzalka lub wskaznik. */
    fun commentIdAt(position: Int): Long? {
        val item = items.getOrNull(position) as? ThreadContextListItem.Content ?: return null
        return (item.row as? EntryListRow.CommentRow)?.comment?.id
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) =
        when (val item = items[position]) {
            is ThreadContextListItem.Arrow -> {
                TYPE_ARROW
            }

            is ThreadContextListItem.Progress -> {
                TYPE_PROGRESS
            }

            is ThreadContextListItem.Content -> {
                when (val row = item.row) {
                    is EntryListRow.EntryRow -> EntryViewHolder.getViewTypeForEntry(row.entry)
                    is EntryListRow.CommentRow -> EntryCommentViewHolder.getViewTypeForEntryComment(row.comment)
                    is EntryListRow.LinkRow -> error("Kontekst watku nie zawiera znalezisk")
                }
            }
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        if (viewType == TYPE_ARROW) {
            ArrowViewHolder(parent.layoutInflater.inflate(R.layout.thread_context_arrow, parent, false))
        } else if (viewType == TYPE_PROGRESS) {
            ProgressViewHolder(parent.layoutInflater.inflate(R.layout.progress_item, parent, false))
        } else {
            constructEntryOrCommentViewHolder(
                parent = parent,
                viewType = viewType,
                userManagerApi = userManagerApi,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = entryActionListener,
                // Naglowek wpisu bez przycisku "odpowiedz" - odpowiadamy paskiem na dole.
                replyListener = null,
                commentActionListener = commentActionListener,
                commentViewListener = commentViewListener,
                onBlockedRevealed = { position -> notifyItemChanged(position) },
            )
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        val item = items[position]
        if (item !is ThreadContextListItem.Content) return

        // Wciecie USTAWIAMY ZAWSZE, takze zerowe - inaczej recykling przeniesie je na inny wiersz.
        applyIndent(holder.itemView, item.depth)

        when (val row = item.row) {
            is EntryListRow.EntryRow -> {
                when (holder) {
                    is EntryViewHolder -> {
                        holder.bindView(
                            entry = row.entry,
                            // Kontekst ma byc czytelny od razu - nie przycinamy wpisu.
                            cutLongEntries = false,
                            openSpoilersDialog = openSpoilersDialog,
                            enableYoutubePlayer = enableYoutubePlayer,
                            enableEmbedPlayer = enableEmbedPlayer,
                            showAdultContent = showAdultContent,
                            hideNsfw = hideNsfw,
                        )
                    }

                    is BlockedViewHolder -> {
                        holder.bindView(row.entry)
                    }
                }
            }

            is EntryListRow.CommentRow -> {
                when (holder) {
                    is EntryCommentViewHolder -> {
                        holder.bindView(
                            row.comment,
                            null,
                            highlightCommentId = highlightCommentId,
                            openSpoilersDialog = openSpoilersDialog,
                            enableYoutubePlayer = enableYoutubePlayer,
                            enableEmbedPlayer = enableEmbedPlayer,
                            showAdultContent = showAdultContent,
                            hideNsfw = hideNsfw,
                        )
                    }

                    is BlockedViewHolder -> {
                        holder.bindView(row.comment)
                    }
                }
            }

            is EntryListRow.LinkRow -> {
                error("Kontekst watku nie zawiera znalezisk")
            }
        }
    }

    /**
     * Wciecie wiersza poddrzewa. Ustawiamy je marginesem na itemView, bez ruszania
     * comment_list_item.xml - ten sam szablon uzywaja inne ekrany.
     */
    private fun applyIndent(
        view: android.view.View,
        depth: Int,
    ) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val indentDp = INDENT_DP.getOrElse(depth) { INDENT_DP.last() }
        val indentPx = (indentDp * view.resources.displayMetrics.density).toInt()
        if (params.marginStart == indentPx) return
        params.marginStart = indentPx
        view.layoutParams = params
    }

    // Ekran nie rysuje separatorow (prepareNoDivider) - granice pozycji wyznaczaja strzalki.
    private class ArrowViewHolder(
        view: android.view.View,
    ) : RecyclerView.ViewHolder(view)

    private class ProgressViewHolder(
        view: android.view.View,
    ) : RecyclerView.ViewHolder(view)

    companion object {
        // Typy wpisu (4-8) i komentarza (9-11) sa zajete przez wspolne viewholdery.
        private const val TYPE_ARROW = 100
        private const val TYPE_PROGRESS = 101

        /**
         * Wciecie narastajace coraz wolniej (12/10/8/6/4 dp) i zatrzymane na 40 dp.
         * Watek schodzi maksymalnie 5 poziomow ponizej celu, wiec nawet na waskim
         * ekranie najglebszy komentarz traci tylko ok. 40 dp szerokosci.
         */
        private val INDENT_DP = intArrayOf(0, 12, 22, 30, 36, 40)
    }
}

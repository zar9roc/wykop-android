package io.github.wykopmobilny.ui.modules.mikroblog.feed.hot.v2

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.databinding.ProgressItemBinding
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.toRows
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.TopCommentsActionListener
import io.github.wykopmobilny.ui.adapters.TopCommentsViewListener
import io.github.wykopmobilny.ui.adapters.bindEntryOrCommentHolder
import io.github.wykopmobilny.ui.adapters.constructEntryOrCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.reactivex.disposables.CompositeDisposable
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.layoutInflater
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi

/**
 * Adapter feedu mikrobloga (V2). Reużywa sprawdzony EntryViewHolder (renderowanie
 * jak dotąd), a paginacja/refresh idą z domeny - stąd własny prosty adapter z listą
 * + stopką ładowania, zamiast starego EndlessProgressAdapter. replyListener=null =>
 * EntryViewHolder w trybie feedu (klik wiersza otwiera szczegóły, brak przycisku
 * odpowiedzi), dokładnie jak stary EntriesAdapter.
 */
internal class MicroblogFeedAdapter(
    private val userManagerApi: UserManagerApi,
    settingsPreferencesApi: SettingsPreferencesApi,
    private val navigator: NewNavigator,
    private val linkHandler: WykopLinkHandler,
    private val entryActionListener: EntryActionListener,
    entryCommentInteractor: EntryCommentInteractor,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val cutLongEntries by lazy { settingsPreferencesApi.cutLongEntries }
    private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
    private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
    private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
    private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
    private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }
    private val showTopComments by lazy { settingsPreferencesApi.showTopComments }

    // Lista wierszy: wpis, a pod nim (gdy wlaczone) jego komentarze jako osobne
    // wiersze - renderowane szablonem odpowiedzi, jak na ekranie wpisu.
    private val rows = mutableListOf<EntryListRow>()
    private var showFooterLoading = false

    private val disposables = CompositeDisposable()
    private val commentActionListener =
        TopCommentsActionListener(entryCommentInteractor, navigator, disposables, ::updateComment)
    private val commentViewListener = TopCommentsViewListener(navigator)

    fun replaceAll(
        newEntries: List<Entry>,
        footerLoading: Boolean,
    ) {
        rows.clear()
        rows.addAll(newEntries.flatMap { it.toRows(includeComments = showTopComments) })
        showFooterLoading = footerLoading
        notifyDataSetChanged()
    }

    fun append(newEntries: List<Entry>) {
        if (newEntries.isEmpty()) return
        val newRows = newEntries.flatMap { it.toRows(includeComments = showTopComments) }
        val insertAt = rows.size
        rows.addAll(newRows)
        notifyItemRangeInserted(insertAt, newRows.size)
    }

    fun setFooterLoading(loading: Boolean) {
        if (showFooterLoading == loading) return
        showFooterLoading = loading
        if (loading) {
            notifyItemInserted(rows.size)
        } else {
            notifyItemRemoved(rows.size)
        }
    }

    fun updateEntry(entry: Entry) {
        val index = rows.indexOfFirst { it is EntryListRow.EntryRow && it.entry.id == entry.id }
        if (index >= 0) {
            rows[index] = EntryListRow.EntryRow(entry)
            notifyItemChanged(index)
        }
    }

    private fun updateComment(comment: EntryComment) {
        val index = rows.indexOfFirst { it is EntryListRow.CommentRow && it.comment.id == comment.id }
        if (index >= 0) {
            rows[index] = EntryListRow.CommentRow(comment)
            notifyItemChanged(index)
        }
    }

    private fun isFooterPosition(position: Int) = showFooterLoading && position == rows.size

    override fun getItemCount(): Int = rows.size + if (showFooterLoading) 1 else 0

    override fun getItemViewType(position: Int): Int =
        if (isFooterPosition(position)) {
            TYPE_LOADING
        } else {
            when (val row = rows[position]) {
                is EntryListRow.EntryRow -> EntryViewHolder.getViewTypeForEntry(row.entry)
                is EntryListRow.CommentRow -> EntryCommentViewHolder.getViewTypeForEntryComment(row.comment)
                is EntryListRow.LinkRow -> error("Feed mikrobloga nie zawiera znalezisk")
            }
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        if (viewType == TYPE_LOADING) {
            LoadingViewHolder(ProgressItemBinding.inflate(parent.layoutInflater, parent, false))
        } else {
            constructEntryOrCommentViewHolder(
                parent = parent,
                viewType = viewType,
                userManagerApi = userManagerApi,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = entryActionListener,
                replyListener = null,
                commentActionListener = commentActionListener,
                commentViewListener = commentViewListener,
                onBlockedRevealed = ::notifyItemChanged,
            )
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        if (isFooterPosition(position)) return
        bindEntryOrCommentHolder(
            holder = holder,
            row = rows[position],
            cutLongEntries = cutLongEntries,
            openSpoilersDialog = openSpoilersDialog,
            enableYoutubePlayer = enableYoutubePlayer,
            enableEmbedPlayer = enableEmbedPlayer,
            showAdultContent = showAdultContent,
            hideNsfw = hideNsfw,
        )
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        disposables.clear()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    private class LoadingViewHolder(
        binding: ProgressItemBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_LOADING = 100
    }
}

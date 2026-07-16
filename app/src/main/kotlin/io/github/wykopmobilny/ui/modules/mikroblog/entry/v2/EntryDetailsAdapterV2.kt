package io.github.wykopmobilny.ui.modules.mikroblog.entry.v2

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.databinding.ProgressItemBinding
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.viewholders.BlockedViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryListener
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentViewListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.layoutInflater
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi

/**
 * Adapter ekranu wpisu V2. W odróżnieniu od starego EntryAdapter trzyma własną,
 * już przefiltrowaną listę komentarzy (filtr czarnej listy stosuje fragment przy
 * mapowaniu), dzięki czemu prepend/append idą przez notifyItemRangeInserted -
 * LinearLayoutManager sam kotwiczy viewport przy wstawkach nad nim.
 *
 * Pozycje: [0] = wpis, [1..n] = komentarze, [n+1] = stopka ładowania (opcjonalna).
 * Wskaźnik doładowywania STARSZYCH stron żyje poza listą (layout fragmentu).
 */
internal class EntryDetailsAdapterV2(
    private val userManagerApi: UserManagerApi,
    settingsPreferencesApi: SettingsPreferencesApi,
    private val navigator: NewNavigator,
    private val linkHandler: WykopLinkHandler,
    private val entryActionListener: EntryActionListener,
    private val commentActionListener: EntryCommentActionListener,
    private val commentViewListener: EntryCommentViewListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val replyListener: EntryListener = { commentViewListener.addReply(it.author) }

    private val cutLongEntries by lazy { settingsPreferencesApi.cutLongEntries }
    private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
    private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
    private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
    private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
    private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }

    var highlightCommentId: Long? = null

    var entry: Entry? = null
        private set

    val comments = mutableListOf<EntryComment>()

    private var showFooterLoading = false

    fun replaceAll(
        newEntry: Entry?,
        newComments: List<EntryComment>,
        footerLoading: Boolean,
    ) {
        entry = newEntry
        comments.clear()
        comments.addAll(newComments)
        showFooterLoading = footerLoading
        notifyDataSetChanged()
    }

    fun updateEntry(newEntry: Entry) {
        entry = newEntry
        notifyItemChanged(0)
    }

    fun prepend(newComments: List<EntryComment>) {
        if (newComments.isEmpty()) return
        comments.addAll(0, newComments)
        notifyItemRangeInserted(1, newComments.size)
    }

    fun append(newComments: List<EntryComment>) {
        if (newComments.isEmpty()) return
        val insertAt = comments.size + 1
        comments.addAll(newComments)
        notifyItemRangeInserted(insertAt, newComments.size)
    }

    fun setFooterLoading(loading: Boolean) {
        if (showFooterLoading == loading) return
        showFooterLoading = loading
        if (loading) {
            notifyItemInserted(comments.size + 1)
        } else {
            notifyItemRemoved(comments.size + 1)
        }
    }

    fun updateComment(comment: EntryComment) {
        val index = comments.indexOfFirst { it.id == comment.id }
        if (index >= 0) {
            comments[index] = comment
            notifyItemChanged(index + 1)
        }
    }

    fun positionOfComment(commentId: Long): Int? =
        comments
            .indexOfFirst { it.id == commentId }
            .takeIf { it >= 0 }
            ?.let { it + 1 }

    override fun getItemCount(): Int {
        entry ?: return 0
        return comments.size + 1 + if (showFooterLoading) 1 else 0
    }

    private fun isFooterPosition(position: Int) = showFooterLoading && position == comments.size + 1

    override fun getItemViewType(position: Int): Int =
        when {
            position == 0 -> EntryViewHolder.getViewTypeForEntry(entry!!)
            isFooterPosition(position) -> TYPE_LOADING
            else -> EntryCommentViewHolder.getViewTypeForEntryComment(comments[position - 1])
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_LOADING -> LoadingViewHolder(ProgressItemBinding.inflate(parent.layoutInflater, parent, false))

            EntryCommentViewHolder.TYPE_BLOCKED,
            EntryViewHolder.TYPE_BLOCKED,
            -> BlockedViewHolder.inflateView(parent, ::notifyItemChanged)

            EntryCommentViewHolder.TYPE_NORMAL,
            EntryCommentViewHolder.TYPE_EMBED,
            ->
                EntryCommentViewHolder.inflateView(
                    parent = parent,
                    viewType = viewType,
                    userManagerApi = userManagerApi,
                    navigator = navigator,
                    linkHandler = linkHandler,
                    commentActionListener = commentActionListener,
                    commentViewListener = commentViewListener,
                    enableClickListener = false,
                )

            else ->
                EntryViewHolder.inflateView(
                    parent = parent,
                    viewType = viewType,
                    userManagerApi = userManagerApi,
                    navigator = navigator,
                    linkHandler = linkHandler,
                    entryActionListener = entryActionListener,
                    replyListener = replyListener,
                )
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (holder) {
            is EntryViewHolder ->
                holder.bindView(
                    entry = entry!!,
                    cutLongEntries = cutLongEntries,
                    openSpoilersDialog = openSpoilersDialog,
                    enableYoutubePlayer = enableYoutubePlayer,
                    enableEmbedPlayer = enableEmbedPlayer,
                    showAdultContent = showAdultContent,
                    hideNsfw = hideNsfw,
                )

            is EntryCommentViewHolder ->
                holder.bindView(
                    comment = comments[position - 1],
                    entryAuthor = entry?.author,
                    highlightCommentId = highlightCommentId ?: 0,
                    openSpoilersDialog = openSpoilersDialog,
                    enableYoutubePlayer = enableYoutubePlayer,
                    enableEmbedPlayer = enableEmbedPlayer,
                    showAdultContent = showAdultContent,
                    hideNsfw = hideNsfw,
                )

            is BlockedViewHolder -> {
                if (position == 0) {
                    holder.bindView(entry!!)
                } else {
                    holder.bindView(comments[position - 1])
                }
            }
        }
    }

    private class LoadingViewHolder(
        binding: ProgressItemBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    companion object {
        // Poza zakresem typów Entry/EntryCommentViewHolder (4..11).
        private const val TYPE_LOADING = 100
    }
}

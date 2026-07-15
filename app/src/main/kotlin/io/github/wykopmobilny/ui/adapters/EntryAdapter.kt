package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import javax.inject.Inject

class EntryAdapter
    @Inject
    constructor(
        private val userManagerApi: UserManagerApi,
        settingsPreferencesApi: SettingsPreferencesApi,
        private val navigator: NewNavigator,
        private val linkHandler: WykopLinkHandler,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        lateinit var entryActionListener: EntryActionListener
        lateinit var commentActionListener: EntryCommentActionListener
        lateinit var commentViewListener: EntryCommentViewListener
        private var replyListener: EntryListener = { commentViewListener.addReply(it.author) }

        var entry: Entry? = null
        var commentId: Long? = null

        private val hideBlacklistedViews by lazy { settingsPreferencesApi.hideBlacklistedViews }
        private val cutLongEntries by lazy { settingsPreferencesApi.cutLongEntries }
        private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
        private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
        private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
        private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
        private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            when (holder) {
                is EntryViewHolder -> {
                    holder.bindView(
                        entry = entry!!,
                        cutLongEntries = cutLongEntries,
                        openSpoilersDialog = openSpoilersDialog,
                        enableYoutubePlayer = enableYoutubePlayer,
                        enableEmbedPlayer = enableEmbedPlayer,
                        showAdultContent = showAdultContent,
                        hideNsfw = hideNsfw,
                    )
                }

                is EntryCommentViewHolder -> {
                    val comment = filteredComments()[position - 1]
                    holder.bindView(
                        comment = comment,
                        entryAuthor = entry?.author,
                        highlightCommentId = commentId ?: 0,
                        openSpoilersDialog = openSpoilersDialog,
                        enableYoutubePlayer = enableYoutubePlayer,
                        enableEmbedPlayer = enableEmbedPlayer,
                        showAdultContent = showAdultContent,
                        hideNsfw = hideNsfw,
                    )
                }

                is BlockedViewHolder -> {
                    if (position == 0) {
                        holder.bindView(entry!!)
                    } else {
                        holder.bindView(filteredComments()[position - 1])
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int =
            if (position == 0) {
                EntryViewHolder.getViewTypeForEntry(entry!!)
            } else {
                EntryCommentViewHolder.getViewTypeForEntryComment(filteredComments()[position - 1])
            }

        override fun getItemCount(): Int {
            entry?.let {
                return filteredComments().size + 1
            }
            return 0
        }

        private fun filteredComments(): List<EntryComment> =
            entry
                ?.comments
                ?.filterNot {
                    hideBlacklistedViews && it.isBlocked && it.deletedReason == null
                }.orEmpty()

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder =
            when (viewType) {
                EntryCommentViewHolder.TYPE_BLOCKED,
                EntryViewHolder.TYPE_BLOCKED,
                -> {
                    BlockedViewHolder.inflateView(parent, ::notifyItemChanged)
                }

                EntryCommentViewHolder.TYPE_NORMAL,
                EntryCommentViewHolder.TYPE_EMBED,
                -> {
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
                }

                else -> {
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
            }

        fun appendComments(newComments: List<EntryComment>) {
            val entry = entry ?: return
            if (newComments.isEmpty()) return
            newComments.forEach { it.entryId = entry.id }
            entry.comments.addAll(newComments)
            // Filtrowanie (hideBlacklistedViews) sprawia, że indeksy po filtrze nie
            // odpowiadają wprost dołożonym elementom - pełne odświeżenie jest tu pewne.
            notifyDataSetChanged()
        }

        fun updateEntry(entry: Entry) {
            this.entry = entry
            notifyItemChanged(0)
        }

        fun updateComment(comment: EntryComment) {
            val position = entry!!.comments.indexOf(comment)
            entry!!.comments[position] = comment
            notifyItemChanged(position + 1)
        }
    }

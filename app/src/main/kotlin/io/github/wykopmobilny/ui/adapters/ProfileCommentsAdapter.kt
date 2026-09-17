package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.base.adapter.EndlessProgressAdapter
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.viewholders.EntryCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import javax.inject.Inject

/**
 * Mieszana lista zakladki "Komentarze" na profilu: wpis-rodzic renderowany zwyklym
 * szablonem wpisu, a pod nim komentarz(e) uzytkownika tym samym szablonem co
 * odpowiedzi na ekranie wpisu.
 */
class ProfileCommentsAdapter
    @Inject
    constructor(
        private val userManagerApi: UserManagerApi,
        private val settingsPreferencesApi: SettingsPreferencesApi,
        private val navigator: NewNavigator,
        private val linkHandler: WykopLinkHandler,
    ) : EndlessProgressAdapter<RecyclerView.ViewHolder, EntryListRow>() {
        // Wymagane pola - ustawiane przez fragment przed zaladowaniem danych.
        lateinit var entryActionListener: EntryActionListener
        lateinit var entryCommentActionListener: EntryCommentActionListener

        private val cutLongEntries by lazy { settingsPreferencesApi.cutLongEntries }
        private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
        private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
        private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
        private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
        private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }
        private val hideBlacklistedViews by lazy { settingsPreferencesApi.hideBlacklistedViews }

        // Odpowiedz/cytat spod komentarza: nie ma tu paska odpowiedzi, wiec przechodzimy
        // na ekran wpisu (zakotwiczony na komentarzu) z gotowa trescia.
        private val commentViewListener = TopCommentsViewListener(navigator)

        override fun getViewType(position: Int) =
            when (val row = dataset[position]!!) {
                is EntryListRow.EntryRow -> EntryViewHolder.getViewTypeForEntry(row.entry)
                is EntryListRow.CommentRow -> EntryCommentViewHolder.getViewTypeForEntryComment(row.comment)
                is EntryListRow.LinkRow -> error("Zakladka komentarzy nie zawiera znalezisk")
            }

        override fun addData(
            items: List<EntryListRow>,
            shouldClearAdapter: Boolean,
        ) {
            super.addData(items.filterNot(::isHiddenByBlacklist), shouldClearAdapter)
        }

        private fun isHiddenByBlacklist(row: EntryListRow) =
            hideBlacklistedViews &&
                when (row) {
                    is EntryListRow.EntryRow -> row.entry.isBlocked
                    // Usuniete komentarze zostaja - maja wlasna adnotacje zamiast tresci.
                    is EntryListRow.CommentRow -> row.comment.isBlocked && row.comment.deletedReason == null
                    is EntryListRow.LinkRow -> false
                }

        override fun constructViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RecyclerView.ViewHolder =
            constructEntryOrCommentViewHolder(
                parent = parent,
                viewType = viewType,
                userManagerApi = userManagerApi,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = entryActionListener,
                replyListener = null,
                commentActionListener = entryCommentActionListener,
                commentViewListener = commentViewListener,
                onBlockedRevealed = ::notifyItemChanged,
            )

        override fun bindHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) = bindEntryOrCommentHolder(
            holder = holder,
            row = dataset[position]!!,
            cutLongEntries = cutLongEntries,
            openSpoilersDialog = openSpoilersDialog,
            enableYoutubePlayer = enableYoutubePlayer,
            enableEmbedPlayer = enableEmbedPlayer,
            showAdultContent = showAdultContent,
            hideNsfw = hideNsfw,
        )

        fun updateComment(comment: EntryComment) {
            val position = dataset.indexOfFirst { it is EntryListRow.CommentRow && it.comment.id == comment.id }
            if (position < 0) return
            dataset[position] = EntryListRow.CommentRow(comment)
            notifyItemChanged(position)
        }

        fun updateEntry(entry: Entry) {
            val position = dataset.indexOfFirst { it is EntryListRow.EntryRow && it.entry.id == entry.id }
            if (position < 0) return
            dataset[position] = EntryListRow.EntryRow(entry)
            notifyItemChanged(position)
        }
    }

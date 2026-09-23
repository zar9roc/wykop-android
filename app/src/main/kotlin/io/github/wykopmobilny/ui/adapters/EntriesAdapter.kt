package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.wykopmobilny.base.adapter.EndlessProgressAdapter
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.toRows
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.viewholders.BlockedViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryListener
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

/**
 * Lista wpisow. Gdy wlaczony jest podglad najlepszych komentarzy, komentarze wpisu
 * trafiaja na liste jako OSOBNE wiersze zaraz pod nim - renderowane tym samym
 * szablonem co odpowiedzi na ekranie wpisu (EntryCommentViewHolder).
 */
class EntriesAdapter
    @Inject
    constructor(
        val userManagerApi: UserManagerApi,
        settingsPreferencesApi: SettingsPreferencesApi,
        val navigator: NewNavigator,
        val linkHandler: WykopLinkHandler,
        entryCommentInteractor: EntryCommentInteractor,
    ) : EndlessProgressAdapter<ViewHolder, EntryListRow>() {
        // Required field, interacts with presenter. Otherwise will throw exception
        lateinit var entryActionListener: EntryActionListener

        var replyListener: EntryListener? = null

        private val hideBlacklistedViews by lazy { settingsPreferencesApi.hideBlacklistedViews }
        private val cutLongEntries by lazy { settingsPreferencesApi.cutLongEntries }
        private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
        private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
        private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
        private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
        private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }
        private val showTopCommentsSetting by lazy { settingsPreferencesApi.showTopComments }

        /** Profil pokazuje najlepsze komentarze zawsze, niezaleznie od ustawienia. */
        var forceShowTopComments: Boolean = false

        private val showTopComments: Boolean
            get() = forceShowTopComments || showTopCommentsSetting

        private val disposables = CompositeDisposable()
        private val commentActionListener =
            TopCommentsActionListener(entryCommentInteractor, navigator, disposables, ::updateComment)
        private val commentViewListener = TopCommentsViewListener(navigator)

        /** Same wpisy (bez wierszy komentarzy) - do podmiany stanu glosu z zewnatrz. */
        val entries: List<Entry>
            get() = data.filterIsInstance<EntryListRow.EntryRow>().map { it.entry }

        fun addEntries(
            items: List<Entry>,
            shouldClearAdapter: Boolean,
        ) = addData(
            items
                .filterNot { hideBlacklistedViews && it.isBlocked }
                .flatMap { entry -> entry.toRows(includeComments = showTopComments) },
            shouldClearAdapter,
        )

        override fun getViewType(position: Int) =
            when (val row = dataset[position]!!) {
                is EntryListRow.EntryRow -> EntryViewHolder.getViewTypeForEntry(row.entry)
                is EntryListRow.CommentRow -> EntryCommentViewHolder.getViewTypeForEntryComment(row.comment)
                is EntryListRow.LinkRow -> error("Lista wpisow nie zawiera znalezisk")
            }

        override fun constructViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder =
            constructEntryOrCommentViewHolder(
                parent = parent,
                viewType = viewType,
                userManagerApi = userManagerApi,
                navigator = navigator,
                linkHandler = linkHandler,
                entryActionListener = entryActionListener,
                replyListener = replyListener,
                commentActionListener = commentActionListener,
                commentViewListener = commentViewListener,
                onBlockedRevealed = ::notifyItemChanged,
            )

        override fun bindHolder(
            holder: ViewHolder,
            position: Int,
        ) = bindEntryOrCommentHolder(
            holder = holder.also { it.markSectionEnd(isSectionEnd(dataset, position)) },
            row = dataset[position]!!,
            cutLongEntries = cutLongEntries,
            openSpoilersDialog = openSpoilersDialog,
            enableYoutubePlayer = enableYoutubePlayer,
            enableEmbedPlayer = enableEmbedPlayer,
            showAdultContent = showAdultContent,
            hideNsfw = hideNsfw,
        )

        fun updateEntry(entry: Entry) {
            val position = dataset.indexOfFirst { it is EntryListRow.EntryRow && it.entry.id == entry.id }
            if (position < 0) return
            dataset[position] = EntryListRow.EntryRow(entry)
            notifyItemChanged(position)
        }

        private fun updateComment(comment: EntryComment) {
            val position = dataset.indexOfFirst { it is EntryListRow.CommentRow && it.comment.id == comment.id }
            if (position < 0) return
            dataset[position] = EntryListRow.CommentRow(comment)
            notifyItemChanged(position)
        }

        override fun onDetachedFromRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
            disposables.clear()
            super.onDetachedFromRecyclerView(recyclerView)
        }
    }

package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.base.adapter.EndlessProgressAdapter
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.viewholders.BlockedViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryCommentViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.LinkViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.SimpleLinkViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.reactivex.disposables.CompositeDisposable
import io.github.wykopmobilny.ui.fragments.links.LinkActionListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import javax.inject.Inject

class EntryLinksAdapter
    @Inject
    constructor(
        private val userManagerApi: UserManagerApi,
        settingsPreferencesApi: SettingsPreferencesApi,
        private val navigator: NewNavigator,
        private val linkHandler: WykopLinkHandler,
        private val appStorage: AppStorage,
        private val linksApi: LinksApi,
        entryCommentInteractor: EntryCommentInteractor,
    ) : EndlessProgressAdapter<ViewHolder, EntryListRow>() {
        // Required field, interacts with presenter. Otherwise will throw exception
        lateinit var entryActionListener: EntryActionListener
        lateinit var linkActionListener: LinkActionListener

        private val linkShowImage by lazy { settingsPreferencesApi.linkShowImage }
        private val showMinifiedImages by lazy { settingsPreferencesApi.showMinifiedImages }
        private val linkSimpleList by lazy { settingsPreferencesApi.linkSimpleList }
        private val linkImagePosition by lazy { settingsPreferencesApi.linkImagePosition }
        private val linkShowAuthor by lazy { settingsPreferencesApi.linkShowAuthor }
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
        private val topCommentsActionListener =
            TopCommentsActionListener(entryCommentInteractor, navigator, disposables, ::updateComment)
        private val topCommentsViewListener = TopCommentsViewListener(navigator)

        /** Same elementy wpis/znalezisko (bez wierszy komentarzy). */
        val items: List<EntryLink>
            get() = data.filterIsInstance<EntryListRow.LinkRow>().map { it.entryLink }

        fun addEntryLinks(
            items: List<EntryLink>,
            shouldClearAdapter: Boolean,
        ) = addData(
            items
                .filterNot {
                    val isBlocked = it.entry?.isBlocked == true || it.link?.isBlocked == true
                    hideBlacklistedViews && isBlocked
                }.flatMap { entryLink ->
                    val entry = entryLink.entry
                    if (entry != null && showTopComments) {
                        // Komentarze wpisu jako osobne wiersze zaraz pod nim.
                        listOf(EntryListRow.LinkRow(entryLink)) +
                            entry.comments.filterNot { it.isBlocked }.map(EntryListRow::CommentRow)
                    } else {
                        listOf(EntryListRow.LinkRow(entryLink))
                    }
                },
            shouldClearAdapter,
        )

        override fun getViewType(position: Int): Int =
            when (val row = dataset[position]!!) {
                is EntryListRow.CommentRow -> EntryCommentViewHolder.getViewTypeForEntryComment(row.comment)
                is EntryListRow.EntryRow -> EntryViewHolder.getViewTypeForEntry(row.entry)
                is EntryListRow.LinkRow ->
                    if (row.entryLink.dataType == EntryLink.TYPE_ENTRY) {
                        EntryViewHolder.getViewTypeForEntry(row.entryLink.entry!!)
                    } else {
                        LinkViewHolder.getViewTypeForLink(
                            row.entryLink.link!!,
                            linkSimpleList = linkSimpleList,
                            linkShowImage = linkShowImage,
                        )
                    }
            }

        override fun constructViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): ViewHolder =
            when (viewType) {
                LinkViewHolder.TYPE_IMAGE, LinkViewHolder.TYPE_NOIMAGE -> {
                    LinkViewHolder.inflateView(
                        parent = parent,
                        viewType = viewType,
                        userManagerApi = userManagerApi,
                        navigator = navigator,
                        linkActionListener = linkActionListener,
                        appStorage = appStorage,
                        linksApi = linksApi,
                        linkImagePosition = linkImagePosition,
                        linkShowAuthor = linkShowAuthor,
                    )
                }

                EntryViewHolder.TYPE_BLOCKED, LinkViewHolder.TYPE_BLOCKED, EntryCommentViewHolder.TYPE_BLOCKED -> {
                    BlockedViewHolder.inflateView(parent, ::notifyItemChanged)
                }

                EntryCommentViewHolder.TYPE_EMBED, EntryCommentViewHolder.TYPE_NORMAL -> {
                    EntryCommentViewHolder.inflateView(
                        parent,
                        viewType,
                        userManagerApi,
                        navigator,
                        linkHandler,
                        topCommentsActionListener,
                        topCommentsViewListener,
                        true,
                    )
                }

                else -> {
                    EntryViewHolder.inflateView(
                        parent,
                        viewType,
                        userManagerApi,
                        navigator,
                        linkHandler,
                        entryActionListener,
                        null,
                    )
                }
            }

        override fun bindHolder(
            holder: ViewHolder,
            position: Int,
        ) {
            val row = dataset[position]!!
            holder.markSectionEnd(isSectionEnd(dataset, position))
            if (row is EntryListRow.CommentRow) {
                when (holder) {
                    is EntryCommentViewHolder ->
                        holder.bindView(
                            row.comment,
                            null,
                            openSpoilersDialog = openSpoilersDialog,
                            enableYoutubePlayer = enableYoutubePlayer,
                            enableEmbedPlayer = enableEmbedPlayer,
                            showAdultContent = showAdultContent,
                            hideNsfw = hideNsfw,
                        )

                    is BlockedViewHolder -> holder.bindView(row.comment)
                }
                return
            }
            val entryLink = (row as EntryListRow.LinkRow).entryLink
            when (holder) {
                is EntryViewHolder -> {
                    entryLink.entry?.let {
                        holder.bindView(
                            entry = it,
                            cutLongEntries = cutLongEntries,
                            openSpoilersDialog = openSpoilersDialog,
                            enableYoutubePlayer = enableYoutubePlayer,
                            enableEmbedPlayer = enableEmbedPlayer,
                            showAdultContent = showAdultContent,
                            hideNsfw = hideNsfw,
                        )
                    }
                }

                is LinkViewHolder -> {
                    entryLink.link?.let {
                        holder.bindView(
                            link = it,
                            linkImagePosition = linkImagePosition,
                            linkShowAuthor = linkShowAuthor,
                        )
                    }
                }

                is BlockedViewHolder -> {
                    entryLink.link?.let(holder::bindView)
                    entryLink.entry?.let(holder::bindView)
                }

                is SimpleLinkViewHolder -> {
                    entryLink.link?.let {
                        holder.bindView(it, showMinifiedImages = showMinifiedImages, linkShowImage = linkShowImage)
                    }
                }
            }
        }

        private fun updateComment(comment: EntryComment) {
            val position = dataset.indexOfFirst { it is EntryListRow.CommentRow && it.comment.id == comment.id }
            if (position < 0) return
            dataset[position] = EntryListRow.CommentRow(comment)
            notifyItemChanged(position)
        }

        fun updateEntry(entry: Entry) {
            val position = dataset.indexOfFirst { it is EntryListRow.LinkRow && it.entryLink.entry?.id == entry.id }
            if (position < 0) return
            (dataset[position] as EntryListRow.LinkRow).entryLink.entry = entry
            notifyItemChanged(position)
        }

        fun updateLink(link: Link) {
            val position = dataset.indexOfFirst { it is EntryListRow.LinkRow && it.entryLink.link?.id == link.id }
            if (position < 0) return
            (dataset[position] as EntryListRow.LinkRow).entryLink.link = link
            notifyItemChanged(position)
        }
    }

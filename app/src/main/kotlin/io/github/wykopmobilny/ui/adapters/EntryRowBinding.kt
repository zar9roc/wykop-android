package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.models.dataclass.EntryListRow
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

/**
 * Wspolne tworzenie i bindowanie wierszy list mieszanych (wpis / komentarz).
 * Typy widokow wpisu (4-8) i komentarza (9-11) sa rozlaczne, wiec jeden adapter
 * obsluguje obie rodziny viewholderow.
 */
fun constructEntryOrCommentViewHolder(
    parent: ViewGroup,
    viewType: Int,
    userManagerApi: UserManagerApi,
    navigator: NewNavigator,
    linkHandler: WykopLinkHandler,
    entryActionListener: EntryActionListener,
    replyListener: EntryListener?,
    commentActionListener: EntryCommentActionListener,
    commentViewListener: EntryCommentViewListener,
    onBlockedRevealed: (Int) -> Unit,
): RecyclerView.ViewHolder =
    when (viewType) {
        EntryViewHolder.TYPE_BLOCKED, EntryCommentViewHolder.TYPE_BLOCKED ->
            BlockedViewHolder.inflateView(parent, onBlockedRevealed)

        EntryCommentViewHolder.TYPE_EMBED, EntryCommentViewHolder.TYPE_NORMAL ->
            EntryCommentViewHolder.inflateView(
                parent,
                viewType,
                userManagerApi,
                navigator,
                linkHandler,
                commentActionListener,
                commentViewListener,
                true,
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

fun bindEntryOrCommentHolder(
    holder: RecyclerView.ViewHolder,
    row: EntryListRow,
    cutLongEntries: Boolean,
    openSpoilersDialog: Boolean,
    enableYoutubePlayer: Boolean,
    enableEmbedPlayer: Boolean,
    showAdultContent: Boolean,
    hideNsfw: Boolean,
) {
    when (row) {
        is EntryListRow.EntryRow ->
            when (holder) {
                is EntryViewHolder ->
                    holder.bindView(
                        entry = row.entry,
                        cutLongEntries = cutLongEntries,
                        openSpoilersDialog = openSpoilersDialog,
                        enableYoutubePlayer = enableYoutubePlayer,
                        enableEmbedPlayer = enableEmbedPlayer,
                        showAdultContent = showAdultContent,
                        hideNsfw = hideNsfw,
                    )

                is BlockedViewHolder -> holder.bindView(row.entry)
            }

        is EntryListRow.CommentRow ->
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

        is EntryListRow.LinkRow -> error("Wiersz znaleziska ma wlasna obsluge w EntryLinksAdapter")
    }
}

package io.github.wykopmobilny.ui.modules.links.linkdetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkCommentLayoutBinding
import io.github.wykopmobilny.databinding.LinkDetailsHeaderLayoutBinding
import io.github.wykopmobilny.databinding.TopLinkCommentLayoutBinding
import io.github.wykopmobilny.links.details.LinkCommentUi
import io.github.wykopmobilny.links.details.LinkDetailsHeaderUi
import io.github.wykopmobilny.links.details.LinkDetailsUi
import io.github.wykopmobilny.links.details.ParentCommentUi
import io.github.wykopmobilny.links.details.RelatedLinksSectionUi
import io.github.wykopmobilny.ui.modules.links.linkdetails.items.bindHeaderV3
import io.github.wykopmobilny.ui.modules.links.linkdetails.items.bindHiddenCommentV3
import io.github.wykopmobilny.ui.modules.links.linkdetails.items.bindHiddenReplyV3
import io.github.wykopmobilny.ui.modules.links.linkdetails.items.bindParentCommentV3
import io.github.wykopmobilny.ui.modules.links.linkdetails.items.bindReplyCommentV3
import io.github.wykopmobilny.utils.asyncDifferConfig

internal class LinkDetailsAdapterV3 :
    ListAdapter<LinkDetailsListItem, LinkDetailsAdapterV3.BindingViewHolder>(
        asyncDifferConfig(LinkDetailsListItem.Diff),
    ) {
    override fun getItemViewType(position: Int) =
        when (val item = getItem(position)) {
            is LinkDetailsListItem.Header -> {
                VIEW_TYPE_HEADER
            }

            is LinkDetailsListItem.ParentComment -> {
                when (item.comment.data) {
                    is LinkCommentUi.Hidden -> VIEW_TYPE_PARENT_COMMENT_HIDDEN
                    is LinkCommentUi.Normal -> VIEW_TYPE_PARENT_COMMENT
                }
            }

            is LinkDetailsListItem.ReplyComment -> {
                when (item.comment) {
                    is LinkCommentUi.Hidden -> VIEW_TYPE_REPLY_COMMENT_HIDDEN
                    is LinkCommentUi.Normal -> VIEW_TYPE_REPLY_COMMENT
                }
            }
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            when (viewType) {
                VIEW_TYPE_HEADER -> LinkDetailsHeaderLayoutBinding.inflate(inflater, parent, false)
                VIEW_TYPE_PARENT_COMMENT -> TopLinkCommentLayoutBinding.inflate(inflater, parent, false)
                VIEW_TYPE_PARENT_COMMENT_HIDDEN -> LinkCommentLayoutBinding.inflate(inflater, parent, false)
                VIEW_TYPE_REPLY_COMMENT -> LinkCommentLayoutBinding.inflate(inflater, parent, false)
                VIEW_TYPE_REPLY_COMMENT_HIDDEN -> LinkCommentLayoutBinding.inflate(inflater, parent, false)
                else -> error("unsupported viewType=$viewType")
            }
        return BindingViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: BindingViewHolder,
        position: Int,
    ) {
        when (val item = getItem(position)) {
            is LinkDetailsListItem.Header -> {
                (holder.binding as LinkDetailsHeaderLayoutBinding).bindHeaderV3(
                    item.header,
                    item.relatedCount,
                )
            }

            is LinkDetailsListItem.ParentComment -> {
                when (val data = item.comment.data) {
                    is LinkCommentUi.Normal -> {
                        (holder.binding as TopLinkCommentLayoutBinding).bindParentCommentV3(
                            parent = item.comment,
                            data = data,
                            hasReplies = item.hasReplies,
                        )
                    }

                    is LinkCommentUi.Hidden -> {
                        (holder.binding as LinkCommentLayoutBinding).bindHiddenCommentV3(
                            item.comment,
                            data,
                        )
                    }
                }
            }

            is LinkDetailsListItem.ReplyComment -> {
                when (val comment = item.comment) {
                    is LinkCommentUi.Normal -> {
                        (holder.binding as LinkCommentLayoutBinding).bindReplyCommentV3(
                            comment,
                            item.isLast,
                        )
                    }

                    is LinkCommentUi.Hidden -> {
                        (holder.binding as LinkCommentLayoutBinding).bindHiddenReplyV3(comment)
                    }
                }
            }
        }
    }

    data class BindingViewHolder(
        val binding: ViewBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PARENT_COMMENT = 1
        private const val VIEW_TYPE_PARENT_COMMENT_HIDDEN = 2
        private const val VIEW_TYPE_REPLY_COMMENT = 3
        private const val VIEW_TYPE_REPLY_COMMENT_HIDDEN = 4
    }
}

internal sealed class LinkDetailsListItem {
    data class Header(
        val header: LinkDetailsHeaderUi,
        val relatedCount: Int = 0,
    ) : LinkDetailsListItem()

    data class ParentComment(
        val comment: ParentCommentUi,
        val hasReplies: Boolean,
    ) : LinkDetailsListItem() {
        val id = comment.data.commentId
    }

    data class ReplyComment(
        val comment: LinkCommentUi,
        val isLast: Boolean,
    ) : LinkDetailsListItem() {
        val id = comment.commentId
    }

    companion object Diff : DiffUtil.ItemCallback<LinkDetailsListItem>() {
        override fun areItemsTheSame(
            oldItem: LinkDetailsListItem,
            newItem: LinkDetailsListItem,
        ): Boolean =
            when (oldItem) {
                is Header -> newItem is Header
                is ParentComment -> oldItem.id == (newItem as? ParentComment)?.id
                is ReplyComment -> oldItem.id == (newItem as? ReplyComment)?.id
            }

        override fun areContentsTheSame(
            oldItem: LinkDetailsListItem,
            newItem: LinkDetailsListItem,
        ) = oldItem == newItem

        override fun getChangePayload(
            oldItem: LinkDetailsListItem,
            newItem: LinkDetailsListItem,
        ) = Change(oldItem, newItem)

        data class Change(
            val old: LinkDetailsListItem,
            val new: LinkDetailsListItem,
        )
    }
}

private val LinkCommentUi.commentId
    get() =
        when (this) {
            is LinkCommentUi.Hidden -> id
            is LinkCommentUi.Normal -> id
        }

@OptIn(ExperimentalStdlibApi::class)
internal fun LinkDetailsUi.toAdapterListV3(): List<LinkDetailsListItem> =
    buildList {
        // Sekcja "Powiazane" nie jest renderowana na liscie - duplikowala przycisk
        // z ikona lancucha w naglowku (relatedCountTextView), ktory otwiera
        // pelny ekran powiazanych. Licznik z sekcji zasila sam przycisk.
        val relatedCount = (relatedSection as? RelatedLinksSectionUi.WithData)?.links?.size ?: 0
        add(LinkDetailsListItem.Header(header, relatedCount))
        commentsSection.comments.forEach { (parent, replies) ->
            add(LinkDetailsListItem.ParentComment(parent, hasReplies = replies.isNotEmpty()))
            addAll(
                replies.map { linkCommentUi ->
                    LinkDetailsListItem.ReplyComment(
                        linkCommentUi,
                        linkCommentUi.commentId == replies.last().commentId,
                    )
                },
            )
        }
    }

package io.github.wykopmobilny.ui.modules.links.linkdetails.items

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.github.wykopmobilny.ui.components.toColorInt
import com.github.wykopmobilny.ui.components.utils.EmbedMediaView
import com.github.wykopmobilny.ui.components.utils.bind
import com.github.wykopmobilny.ui.components.utils.readColorAttr
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkCommentLayoutBinding
import io.github.wykopmobilny.databinding.TopLinkCommentLayoutBinding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.links.details.LinkCommentUi
import io.github.wykopmobilny.links.details.ParentCommentUi
import io.github.wykopmobilny.ui.components.widgets.ColorConst
import io.github.wykopmobilny.ui.components.widgets.EmbedMediaUi
import io.github.wykopmobilny.utils.bindings.setOnClick
import io.github.wykopmobilny.utils.bindings.setOnLongClick
import io.github.wykopmobilny.utils.textview.BetterLinkMovementMethod
import androidx.appcompat.R as AppcompatR
import io.github.wykopmobilny.ui.base.android.R as BaseR

// Delikatne zolte tlo komentarza docelowego (nawigacja z powiadomienia) - ~15% alpha,
// czytelne na jasnym i ciemnym motywie.
private val linkedCommentBackground = 0x26FFC107.toInt()

// Layouty komentarzy maja ViewStub ze starym WykopEmbedView (uzywany przez adaptery v2) -
// dla bindingow v3 podmieniamy layout stuba na EmbedMediaView z nowej architektury.
// Po pierwszym inflate stub znika z hierarchii, wiec kolejne bindy znajduja widok po id.
private fun bindEmbedV3(
    root: View,
    stub: ViewStub,
    embed: EmbedMediaUi?,
    commentId: Long,
) {
    val contentUrl =
        when (val content = embed?.content) {
            is EmbedMediaUi.Content.StaticImage -> content.url
            is EmbedMediaUi.Content.PlayableMedia -> content.previewImage
            null -> null
        }
    DiagnosticCheckpoint.log(
        "CommentEmbedV3",
        "id=$commentId embed=${embed?.content?.javaClass?.simpleName} url=$contentUrl stubParent=${stub.parent != null}",
    )
    val current = root.findViewById<View>(R.id.wykopEmbedView)
    val embedView =
        when {
            current is EmbedMediaView -> current
            embed != null -> {
                stub.layoutResource = R.layout.stub_embed_media
                (stub.inflate() as EmbedMediaView).apply {
                    // Stub w XML ma wrap_content x wrap_content - karta bez zaladowanego
                    // obrazka mierzy sie wtedy na 0x0. Pelna szerokosc z constraintow.
                    updateLayoutParams<ConstraintLayout.LayoutParams> {
                        width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                        height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    }
                }
            }
            else -> null
        }
    embedView?.bind(embed)
}

private fun hideEmbedV3(root: View) {
    (root.findViewById<View>(R.id.wykopEmbedView) as? EmbedMediaView)?.isVisible = false
}

// Zwiniety watek: strzalka w dol (mozna rozwinac), rozwiniety: w gore (mozna zwinac).
private fun Context.chevronFor(collapsed: Boolean): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(if (collapsed) R.attr.expandDrawable else R.attr.collapseDrawable, typedValue, true)
    return typedValue.resourceId
}

// --- Parent comment (top_link_comment_layout.xml) ---

@Suppress("UnusedParameter")
internal fun TopLinkCommentLayoutBinding.bindParentCommentV3(
    parent: ParentCommentUi,
    data: LinkCommentUi.Normal,
    hasReplies: Boolean,
) {
    root.setOnClick(data.clickAction)
    root.setOnLongClick(parent.toggleExpansionStateAction)
    if (data.showsOption) {
        root.setBackgroundColor(
            root.context.readColorAttr(AppcompatR.attr.colorControlHighlight).defaultColor,
        )
    } else if (data.badge == ColorConst.CommentLinked) {
        root.setBackgroundColor(linkedCommentBackground)
    } else {
        root.background = null
    }

    // Collapse button
    collapseButtonImageView.isVisible = parent.toggleExpansionStateAction != null
    collapseButtonImageView.setOnClick(parent.toggleExpansionStateAction)
    collapseButtonImageView.setImageResource(root.context.chevronFor(collapsed = parent.collapsedCount != null))

    // Author — bind AuthorHeaderView's internal views
    val authorAvatarView = authorHeaderView.findViewById<android.view.View>(R.id.authorAvatarView)
    authorAvatarView?.bindAvatarV3(data.author.avatar)
    authorAvatarView?.setOnClick(data.profileAction)
    val nameView = authorHeaderView.findViewById<TextView>(R.id.userNameTextView)
    if (nameView != null) {
        nameView.text = data.author.name
        val authorColor =
            data.author.color?.toColorInt(nameView.context)
                ?: nameView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
        nameView.setTextColor(authorColor)
        nameView.setOnClick(data.profileAction)
    }
    val dateView = authorHeaderView.findViewById<TextView>(R.id.entryDateTextView)
    if (dateView != null) {
        dateView.text = data.app?.let { "${data.postedAgo} via $it" } ?: data.postedAgo
    }

    // Body
    commentContentTextView.text = data.body.content
    commentContentTextView.isVisible = data.body.content != null
    // Spany z domeny (wzmianki/tagi/URL-e) potrzebuja movement method, zeby dostawac
    // klikniecia; dotyk poza linkiem propaguje sie do klikniecia wiersza.
    BetterLinkMovementMethod.linkifyHtml(commentContentTextView)

    // Embed
    bindEmbedV3(root, wykopEmbedView, data.embed, data.id)

    // Badge
    authorBadgeStripView.setBackgroundColor(
        data.badge.toColorInt(context = root.context).defaultColor,
    )

    // Vote buttons — bind directly without VoteButton.setup()
    plusVoteButton.text = data.plusCount.label
    val plusColor =
        data.plusCount.color?.toColorInt(plusVoteButton.context)
            ?: plusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    plusVoteButton.setTextColor(plusColor)
    plusVoteButton.setOnClick(data.plusCount.clickAction)

    minusVoteButton.text = data.minusCount.label
    val minusColor =
        data.minusCount.color?.toColorInt(minusVoteButton.context)
            ?: minusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    minusVoteButton.setTextColor(minusColor)
    minusVoteButton.setOnClick(data.minusCount.clickAction)

    // Action buttons
    shareTextView.setOnClick(data.shareAction)
    moreOptionsTextView.setOnClick(data.moreAction)

    // Message text (hidden state indicator) — hide for normal comments
    messageTextView.isVisible = false
}

// --- Reply comment (link_comment_layout.xml) ---

@Suppress("UnusedParameter")
internal fun LinkCommentLayoutBinding.bindReplyCommentV3(
    comment: LinkCommentUi.Normal,
    isLast: Boolean,
) {
    root.setOnClick(comment.clickAction)
    if (comment.showsOption) {
        root.setBackgroundColor(
            root.context.readColorAttr(AppcompatR.attr.colorControlHighlight).defaultColor,
        )
    } else if (comment.badge == ColorConst.CommentLinked) {
        root.setBackgroundColor(linkedCommentBackground)
    } else {
        root.background = null
    }

    // Author
    avatarView.bindAvatarV3(comment.author.avatar)
    avatarView.setOnClick(comment.profileAction)
    authorTextView.text = comment.author.name
    val authorColor =
        comment.author.color?.toColorInt(authorTextView.context)
            ?: authorTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    authorTextView.setTextColor(authorColor)

    // Date
    dateTextView.text = comment.app?.let { "${comment.postedAgo} via $it" } ?: comment.postedAgo

    // Collapse button — visible for reply comments
    collapseButtonImageView.isVisible = false

    // Body
    commentContentTextView.text = comment.body.content
    commentContentTextView.isVisible = comment.body.content != null
    BetterLinkMovementMethod.linkifyHtml(commentContentTextView)

    // Embed
    bindEmbedV3(root, wykopEmbedView, comment.embed, comment.id)

    // Badge
    val badgeColor =
        (
            comment.badge?.toColorInt(context = root.context)
                ?: root.context.readColorAttr(BaseR.attr.colorDivider)
        ).defaultColor
    authorBadgeStripView.setBackgroundColor(badgeColor)

    // Vote buttons
    plusVoteButton.text = comment.plusCount.label
    val plusColor =
        comment.plusCount.color?.toColorInt(plusVoteButton.context)
            ?: plusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    plusVoteButton.setTextColor(plusColor)
    plusVoteButton.setOnClick(comment.plusCount.clickAction)

    minusVoteButton.text = comment.minusCount.label
    val minusColor =
        comment.minusCount.color?.toColorInt(minusVoteButton.context)
            ?: minusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    minusVoteButton.setTextColor(minusColor)
    minusVoteButton.setOnClick(comment.minusCount.clickAction)

    // Action buttons
    shareTextView.setOnClick(comment.shareAction)
    moreOptionsTextView.setOnClick(comment.moreAction)
}

// --- Hidden parent comment (using link_comment_layout.xml) ---

internal fun LinkCommentLayoutBinding.bindHiddenCommentV3(
    parent: ParentCommentUi,
    data: LinkCommentUi.Hidden,
) {
    root.setOnClick(data.onClicked)
    root.setOnLongClick(parent.toggleExpansionStateAction)

    // Author
    avatarView.bindAvatarV3(data.author.avatar)
    authorTextView.text = data.author.name
    val authorColor =
        data.author.color?.toColorInt(authorTextView.context)
            ?: authorTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    authorTextView.setTextColor(authorColor)

    // Collapse count
    collapseButtonImageView.isVisible = parent.collapsedCount != null
    collapseButtonImageView.setOnClick(parent.toggleExpansionStateAction)
    collapseButtonImageView.setImageResource(root.context.chevronFor(collapsed = parent.collapsedCount != null))

    // Badge
    val badgeColor = data.badge.toColorInt(context = root.context).defaultColor
    authorBadgeStripView.setBackgroundColor(badgeColor)

    // Hide content views
    commentContentTextView.isVisible = false
    dateTextView.isVisible = false
    patronBadgeTextView.isVisible = false
    dotTextView.isVisible = false
    shareTextView.isVisible = false
    quoteTextView.isVisible = false
    replyTextView.isVisible = false
    plusVoteButton.isVisible = false
    minusVoteButton.isVisible = false
    moreOptionsTextView.isVisible = false
    hideEmbedV3(root)
}

// --- Hidden reply comment (using link_comment_layout.xml) ---

internal fun LinkCommentLayoutBinding.bindHiddenReplyV3(comment: LinkCommentUi.Hidden) {
    root.setOnClick(comment.onClicked)

    // Author
    avatarView.bindAvatarV3(comment.author.avatar)
    authorTextView.text = comment.author.name
    val authorColor =
        comment.author.color?.toColorInt(authorTextView.context)
            ?: authorTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    authorTextView.setTextColor(authorColor)

    // Badge
    val badgeColor =
        (
            comment.badge?.toColorInt(context = root.context)
                ?: root.context.readColorAttr(BaseR.attr.colorDivider)
        ).defaultColor
    authorBadgeStripView.setBackgroundColor(badgeColor)

    // Hide content views
    commentContentTextView.isVisible = false
    dateTextView.isVisible = false
    collapseButtonImageView.isVisible = false
    patronBadgeTextView.isVisible = false
    dotTextView.isVisible = false
    shareTextView.isVisible = false
    quoteTextView.isVisible = false
    replyTextView.isVisible = false
    plusVoteButton.isVisible = false
    minusVoteButton.isVisible = false
    moreOptionsTextView.isVisible = false
    hideEmbedV3(root)
}

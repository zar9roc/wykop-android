package io.github.wykopmobilny.ui.modules.links.linkdetails.items

import android.widget.TextView
import androidx.core.view.isVisible
import com.github.wykopmobilny.ui.components.toColorInt
import com.github.wykopmobilny.ui.components.utils.readColorAttr
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkCommentLayoutBinding
import io.github.wykopmobilny.databinding.TopLinkCommentLayoutBinding
import io.github.wykopmobilny.links.details.LinkCommentUi
import io.github.wykopmobilny.links.details.ParentCommentUi
import io.github.wykopmobilny.utils.bindings.setOnClick
import io.github.wykopmobilny.utils.bindings.setOnLongClick
import androidx.appcompat.R as AppcompatR
import io.github.wykopmobilny.ui.base.android.R as BaseR

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
    } else {
        root.background = null
    }

    // Collapse button
    collapseButtonImageView.isVisible = parent.toggleExpansionStateAction != null
    collapseButtonImageView.setOnClick(parent.toggleExpansionStateAction)

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

    // Embed — skip ViewStub for now (handled in Etap 2)

    // Badge
    authorBadgeStripView.setBackgroundColor(
        data.badge.toColorInt(context = root.context).defaultColor,
    )

    // Vote buttons — bind directly without VoteButton.setup()
    plusVoteButton.text = data.plusCount.label
    val plusColor =
        data.plusCount.color?.toColorInt(plusVoteButton.context)
            ?: plusVoteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    plusVoteButton.setTextColor(plusColor)
    plusVoteButton.setOnClick(data.plusCount.clickAction)

    minusVoteButton.text = data.minusCount.label
    val minusColor =
        data.minusCount.color?.toColorInt(minusVoteButton.context)
            ?: minusVoteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
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
            ?: plusVoteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    plusVoteButton.setTextColor(plusColor)
    plusVoteButton.setOnClick(comment.plusCount.clickAction)

    minusVoteButton.text = comment.minusCount.label
    val minusColor =
        comment.minusCount.color?.toColorInt(minusVoteButton.context)
            ?: minusVoteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
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
}

package io.github.wykopmobilny.links.details.items

import androidx.core.view.isVisible
import com.github.wykopmobilny.ui.components.bind
import com.github.wykopmobilny.ui.components.setUserNick
import com.github.wykopmobilny.ui.components.toColorInt
import com.github.wykopmobilny.ui.components.utils.bind
import com.github.wykopmobilny.ui.components.utils.readColorAttr
import io.github.wykopmobilny.links.details.LinkCommentUi
import io.github.wykopmobilny.ui.link_details.android.databinding.LinkDetailsReplyCommentBinding
import io.github.wykopmobilny.ui.link_details.android.databinding.LinkDetailsReplyCommentHiddenBinding
import io.github.wykopmobilny.utils.bindings.setOnClick
import androidx.appcompat.R as AppcompatR
import io.github.wykopmobilny.ui.base.android.R as BaseR

internal fun LinkDetailsReplyCommentHiddenBinding.bindHiddenReply(comment: LinkCommentUi.Hidden) {
    root.setOnClick(comment.onClicked)
    txtUser.setUserNick(comment.author)
    val badgeColor =
        (comment.badge?.toColorInt(context = root.context) ?: root.context.readColorAttr(BaseR.attr.colorDivider))
            .defaultColor
    imgBadge.setBackgroundColor(badgeColor)
}

internal fun LinkDetailsReplyCommentBinding.bindReplyComment(
    comment: LinkCommentUi.Normal,
    isLast: Boolean,
) {
    clickableContainer.setOnClick(comment.clickAction)
    if (comment.showsOption) {
        clickableContainer.setBackgroundColor(clickableContainer.context.readColorAttr(AppcompatR.attr.colorControlHighlight).defaultColor)
    } else {
        clickableContainer.background = null
    }
    imgAvatar.bind(comment.author.avatar)
    txtUser.setUserNick(comment.author)
    txtTimestamp.text = comment.postedAgo
    txtApp.text = comment.app?.let { "via $it" }
    txtBody.bind(comment.body)
    imgEmbed.bind(comment.embed)
    val badgeColor =
        (comment.badge?.toColorInt(context = root.context) ?: root.context.readColorAttr(BaseR.attr.colorDivider))
            .defaultColor
    lineCommentMiddle.setBackgroundColor(badgeColor)
    lineCommentLast.setBackgroundColor(badgeColor)
    lineAlwaysVisible.setBackgroundColor(badgeColor)
    lineHorizontal.setBackgroundColor(badgeColor)
    plusButton.bind(comment.plusCount)
    minusButton.bind(comment.minusCount)
    lineCommentMiddle.isVisible = !isLast
    lineCommentLast.isVisible = isLast
}

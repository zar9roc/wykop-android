package io.github.wykopmobilny.ui.fragments.linkcomments

import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item

interface LinkCommentViewListener {
    fun replyComment(comment: LinkCommentV3Item)

    fun quoteComment(comment: LinkCommentV3Item)

    fun setCollapsed(
        comment: LinkCommentV3Item,
        isCollapsed: Boolean,
    )
}

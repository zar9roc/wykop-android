package io.github.wykopmobilny.ui.fragments.linkcomments

import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item

interface LinkCommentActionListener {
    fun digComment(comment: LinkCommentV3Item)

    fun buryComment(comment: LinkCommentV3Item)

    fun removeVote(comment: LinkCommentV3Item)

    fun deleteComment(comment: LinkCommentV3Item)
}

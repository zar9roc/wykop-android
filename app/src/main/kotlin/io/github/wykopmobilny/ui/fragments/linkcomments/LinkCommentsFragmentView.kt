package io.github.wykopmobilny.ui.fragments.linkcomments

import io.github.wykopmobilny.base.BaseView
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item

interface LinkCommentsFragmentView : BaseView {
    var showSearchEmptyView: Boolean

    fun updateComment(comment: LinkCommentV3Item)

    fun disableLoading()

    fun addItems(
        items: List<LinkCommentV3Item>,
        shouldRefresh: Boolean = false,
    )
}

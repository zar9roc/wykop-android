package io.github.wykopmobilny.ui.fragments.linkcomments

import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item

interface LinkCommentViewListener {
    fun replyComment(comment: LinkCommentV3Item)

    fun quoteComment(comment: LinkCommentV3Item)

    fun setCollapsed(
        comment: LinkCommentV3Item,
        isCollapsed: Boolean,
    )

    /**
     * Tapniecie w komentarz/odpowiedz. Domyslnie nic - na ekranie z komentarzami jestesmy
     * juz na miejscu; listy (profil) przenosza do znaleziska zakotwiczonego na komentarzu.
     */
    fun openComment(comment: LinkCommentV3Item) = Unit
}

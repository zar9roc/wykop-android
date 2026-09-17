package io.github.wykopmobilny.ui.fragments.entrycomments

import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.EntryComment

interface EntryCommentViewListener {
    // Caly komentarz (nie sam autor) - listy poza ekranem wpisu potrzebuja jeszcze
    // entryId, zeby wiedziec, ktory wpis otworzyc.
    fun addReply(comment: EntryComment)

    /** Odpowiedz pod samym wpisem (przycisk w naglowku na ekranie wpisu). */
    fun addReplyToAuthor(author: Author)

    fun quoteComment(comment: EntryComment)
}

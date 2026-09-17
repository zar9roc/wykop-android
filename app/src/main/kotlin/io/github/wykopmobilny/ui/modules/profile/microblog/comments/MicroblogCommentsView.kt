package io.github.wykopmobilny.ui.modules.profile.microblog.comments

import io.github.wykopmobilny.base.BaseView
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.Voter

/**
 * Zakladka "Komentarze" na profilu - lista mieszana (wpis + komentarze pod nim),
 * stad akcje zarowno na wpisach, jak i na komentarzach.
 */
interface MicroblogCommentsView : BaseView {
    var showSearchEmptyView: Boolean

    fun updateComment(comment: EntryComment)

    fun updateEntry(entry: Entry)

    fun disableLoading()

    fun showVoters(voters: List<Voter>)

    fun openVotersMenu()

    fun addItems(
        items: List<EntryListRow>,
        shouldRefresh: Boolean = false,
    )
}

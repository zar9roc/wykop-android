package io.github.wykopmobilny.ui.fragments.entries

import io.github.wykopmobilny.models.dataclass.Entry

interface EntryActionListener {
    fun voteEntry(entry: Entry)

    fun unvoteEntry(entry: Entry)

    fun markFavorite(entry: Entry)

    fun deleteEntry(entry: Entry)

    // Toggle obserwowania dyskusji we wpisie (powiadomienia o nowych komentarzach).
    fun observeDiscussion(entry: Entry)

    fun voteSurvey(
        entry: Entry,
        index: Int,
    )

    fun getVoters(entry: Entry)
}

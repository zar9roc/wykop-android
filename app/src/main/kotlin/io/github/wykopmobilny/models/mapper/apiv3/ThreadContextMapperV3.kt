package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.entries.ThreadAncestorResponseV3
import io.github.wykopmobilny.kotlin.convertWykopContentToHtml
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryListRow

/**
 * Elementy watku (/v3/entries-threads) na modele domenowe, zeby ekran kontekstu
 * renderowal je tymi samymi viewholderami co reszta aplikacji (lista mieszana
 * wpis + komentarze).
 */
object ThreadContextMapperV3 {
    fun mapRow(
        value: ThreadAncestorResponseV3,
        entryId: Long,
        owmContentFilter: OWMContentFilter,
        // Nick autora wpisu - znany dopiero po zmapowaniu pierwszego elementu sciezki,
        // wiec repozytorium podaje go z zewnatrz (dla samego wpisu nie jest potrzebny).
        entryAuthorNick: String? = null,
    ): EntryListRow =
        if (value.isEntry) {
            EntryListRow.EntryRow(mapEntry(value, owmContentFilter))
        } else {
            EntryListRow.CommentRow(mapComment(value, entryId, owmContentFilter, entryAuthorNick))
        }

    private fun mapEntry(
        value: ThreadAncestorResponseV3,
        owmContentFilter: OWMContentFilter,
    ) = owmContentFilter.filterEntry(
        Entry(
            id = value.id,
            author = AuthorMapperV3.map(value.author),
            body = value.content.orEmpty().convertWykopContentToHtml(),
            fullDate = value.createdAt,
            isVoted = (value.voted ?: 0) > 0,
            isFavorite = value.favourite ?: false,
            // Watek nie zwraca ankiety - na ekranie kontekstu i tak jej nie glosujemy.
            survey = null,
            embed = value.media?.let { MediaMapperV3.map(it, adult = value.adult ?: false) },
            voteCount = (value.votes?.up ?: 0) - (value.votes?.down ?: 0),
            commentsCount = value.comments?.total ?: value.comments?.count ?: 0,
            comments = mutableListOf(),
            app = value.device?.takeIf { it.isNotEmpty() },
            violationUrl = null,
            isNsfw = value.content?.lowercase()?.contains("#nsfw") == true,
            isBlocked = false,
            // Kontekst ma byc czytelny od razu - nie zwijamy tresci wpisu.
            collapsed = false,
            isCommentingPossible = true,
            isObservedDiscussion = value.observedDiscussion ?: false,
            attachments = value.media?.let { MediaMapperV3.mapAttachments(it, adult = value.adult ?: false) }.orEmpty(),
        ),
    )

    private fun mapComment(
        value: ThreadAncestorResponseV3,
        entryId: Long,
        owmContentFilter: OWMContentFilter,
        entryAuthorNick: String?,
    ) = owmContentFilter.filterEntryComment(
        EntryComment(
            id = value.id,
            entryId = entryId,
            author = AuthorMapperV3.map(value.author),
            body = value.content.orEmpty().convertWykopContentToHtml(),
            fullDate = value.createdAt,
            isVoted = (value.voted ?: 0) > 0,
            embed = value.media?.let { MediaMapperV3.map(it, adult = value.adult ?: false) },
            voteCount = (value.votes?.up ?: 0) - (value.votes?.down ?: 0),
            app = value.device?.takeIf { it.isNotEmpty() },
            violationUrl = null,
            isNsfw = value.content?.lowercase()?.contains("#nsfw") == true,
            isBlocked = false,
            deletedReason = value.deletedReason,
            slug = value.slug,
            entryAuthorNick = entryAuthorNick,
            attachments = value.media?.let { MediaMapperV3.mapAttachments(it, adult = value.adult ?: false) }.orEmpty(),
        ),
    )
}

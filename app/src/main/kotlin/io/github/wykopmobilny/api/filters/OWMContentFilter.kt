package io.github.wykopmobilny.api.filters

import io.github.wykopmobilny.api.patrons.PatronsApi
import io.github.wykopmobilny.api.patrons.getBadgeFor
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.utils.textview.removeHtml
import javax.inject.Inject

class OWMContentFilter
    @Inject
    constructor(
        private val appStorage: AppStorage,
        private val settingsPreferencesApi: SettingsPreferencesApi,
        private val patronsApi: PatronsApi,
    ) {
        fun filterEntry(entry: Entry) =
            entry.apply {
                author.badge = patronsApi.getBadgeFor(author)
                isBlocked =
                    isBlocked ||
                    body.bodyContainsBlockedTags() ||
                    author.nick.isUserBlocked() ||
                    (settingsPreferencesApi.hideLowRangeAuthors && author.group == 0) ||
                    (settingsPreferencesApi.hideContentWithoutTags && !body.bodyContainsTags())
            }

        fun filterEntryComment(comment: EntryComment) =
            comment.apply {
                author.badge = patronsApi.getBadgeFor(author)
                isBlocked =
                    isBlocked ||
                    body.bodyContainsBlockedTags() ||
                    author.nick.isUserBlocked() ||
                    (settingsPreferencesApi.hideLowRangeAuthors && author.group == 0)
            }

        fun filterLinkComment(comment: LinkCommentV3Item) =
            comment.apply {
                author.badge = patronsApi.getBadgeFor(author)
                isBlocked =
                    isBlocked ||
                    body?.bodyContainsBlockedTags() ?: false ||
                    author.nick.isUserBlocked() ||
                    (settingsPreferencesApi.hideLowRangeAuthors && author.group == 0)
            }

        fun filterLink(link: Link) =
            link.apply {
                gotSelected = appStorage.linksQueries.contains(linkId = id).executeAsOne() > 0
                isBlocked =
                    isBlocked ||
                    tags.bodyContainsBlockedTags() ||
                    author?.nick?.isUserBlocked() ?: false ||
                    (settingsPreferencesApi.hideLowRangeAuthors && author?.group == 0)
            }

        // [a-z] lapal tylko male litery - tagi z wielka litera (#Wordziel) albo polskimi
        // znakami byly niewykrywane. \p{L} + IGNORE_CASE obejmuje wszystkie litery.
        private val tagsRegex = "(^|\\s)(#[\\p{L}\\d_-]+)".toRegex(RegexOption.IGNORE_CASE)

        private fun String.bodyContainsTags() = tagsRegex.containsMatchIn(this.removeHtml())

        private fun String.bodyContainsBlockedTags(): Boolean {
            val blockedTags = appStorage.blacklistQueries.allTags().executeAsList()
            if (blockedTags.isEmpty()) return false
            val blockedLower = blockedTags.map { it.lowercase() }
            // matchEntire wymagalo, by CALA tresc byla jednym tagiem - dla realnych
            // wpisow (tekst + tagi) zwracalo null i blokada nigdy nie dzialala.
            // findAll wyciaga WSZYSTKIE tagi z tresci; grupa 2 = "#tag".
            val tagsInBody =
                tagsRegex
                    .findAll(this.removeHtml())
                    .map { it.groupValues[2].removePrefix("#").lowercase() }
                    .toSet()
            return tagsInBody.any { it in blockedLower }
        }

        private fun String.isUserBlocked(): Boolean {
            val nick = this.removePrefix("@").lowercase()
            return appStorage.blacklistQueries
                .allProfiles()
                .executeAsList()
                .any { it.lowercase() == nick }
        }
    }

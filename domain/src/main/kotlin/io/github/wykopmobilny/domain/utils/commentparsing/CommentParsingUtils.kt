package io.github.wykopmobilny.domain.utils.commentparsing

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.domain.navigation.InteropRequest
import io.github.wykopmobilny.domain.navigation.WykopTextUtils
import io.github.wykopmobilny.domain.navigation.WykopTextUtils.RecognizedLink
import io.github.wykopmobilny.kotlin.AppDispatchers
import io.github.wykopmobilny.kotlin.convertWykopContentToHtml
import io.github.wykopmobilny.kotlin.linkifyTagsAndMentions
import kotlinx.coroutines.withContext

internal suspend fun String?.toCommentBody(
    textUtils: WykopTextUtils,
    expandedSpoilers: Set<ExpandedSpoiler>,
    showsSpoilersInDialog: Boolean,
    saveExpandedSpoiler: (ExpandedSpoiler) -> Unit,
    showSpoilerDialog: (suspend () -> CharSequence) -> Unit,
    onNavigation: (InteropRequest) -> Unit,
): CharSequence? =
    withContext(AppDispatchers.Default) {
        if (isNullOrBlank()) return@withContext null

        // Tresc z API v3 to markdown z golymi @loginami/#tagami/URL-ami - parseHtml
        // tworzy klikalne spany tylko z tagow <a>, wiec najpierw budujemy HTML.
        textUtils.parseHtml(
            text = this@toCommentBody.convertWykopContentToHtml().linkifyTagsAndMentions(),
            onLinkClicked = { link ->
                when (link) {
                    is RecognizedLink.Profile -> {
                        onNavigation(InteropRequest.Profile(link.profileId))
                    }

                    is RecognizedLink.Tag -> {
                        onNavigation(InteropRequest.Tag(link.tagId))
                    }

                    is RecognizedLink.Spoiler -> {
                        Napier.w("Unexpected spoiler link: ${link.id}")
                    }

                    is RecognizedLink.Other -> {
                        onNavigation(InteropRequest.WebBrowser(link.url))
                    }
                }
            },
        )
    }

internal suspend fun String.copyableText(textUtils: WykopTextUtils) =
    withContext(AppDispatchers.Default) {
        textUtils.parseHtml(this@copyableText).toString()
    }

internal typealias ExpandedSpoiler = String

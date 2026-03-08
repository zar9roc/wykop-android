package io.github.wykopmobilny.api.responses.v3.observed

import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3

sealed class ObservedItemV3 {
    data class EntryItem(
        val entry: EntryResponseV3,
    ) : ObservedItemV3()

    data class LinkItem(
        val link: LinkResponseV3,
    ) : ObservedItemV3()
}

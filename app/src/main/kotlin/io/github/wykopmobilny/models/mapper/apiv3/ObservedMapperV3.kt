package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import io.github.wykopmobilny.models.dataclass.EntryLink

fun ObservedItemV3.toEntryLink(owmContentFilter: OWMContentFilter): EntryLink =
    when (this) {
        is ObservedItemV3.EntryItem -> {
            EntryLink(
                link = null,
                entry = entry.filterEntryV3(owmContentFilter),
            )
        }

        is ObservedItemV3.LinkItem -> {
            EntryLink(
                link = link.filterLinkV3(owmContentFilter),
                entry = null,
            )
        }
    }

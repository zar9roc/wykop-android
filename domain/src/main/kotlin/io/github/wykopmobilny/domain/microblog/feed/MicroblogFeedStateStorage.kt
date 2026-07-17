package io.github.wykopmobilny.domain.microblog.feed

import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedScope
import io.github.wykopmobilny.entries.feed.MicroblogFeedSort
import io.github.wykopmobilny.ui.base.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@MicroblogFeedScope
internal class MicroblogFeedStateStorage
    @Inject
    constructor() {
        val state = MutableStateFlow(MicroblogFeedState())

        fun update(updater: (MicroblogFeedState) -> MicroblogFeedState) {
            state.update(updater)
        }
    }

internal data class MicroblogFeedState(
    val entries: List<EntryResponseV3> = emptyList(),
    val sort: MicroblogFeedSort = MicroblogFeedSort.NEWEST,
    // Kursor kolejnej strony (feedy v3 stronicują opaque Stringiem, nie numerem).
    val nextPage: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loaded: Boolean = false,
    val generalResource: Resource = Resource.idle(),
)

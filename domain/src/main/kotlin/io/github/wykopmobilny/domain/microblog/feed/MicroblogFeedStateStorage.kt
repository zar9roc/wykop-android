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
    // Kursor kolejnej strony. API v3 często NIE zwraca pola `next` dla feedów, więc
    // fallbackujemy na numer strony (jak stary HotPresenter: next ?: ++pageNumber).
    val nextPage: String? = null,
    val pageNumber: Int = 1,
    // Dopóki ostatnia strona zwróciła jakieś wpisy - zakładamy, że może być więcej
    // (stop na pustej stronie). Nie polegamy na `next`, bo bywa null przy 14 stronach.
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loaded: Boolean = false,
    val generalResource: Resource = Resource.idle(),
)

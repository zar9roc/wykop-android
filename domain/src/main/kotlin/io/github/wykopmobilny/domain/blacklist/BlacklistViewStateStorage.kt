package io.github.wykopmobilny.domain.blacklist

import io.github.wykopmobilny.domain.blacklist.di.BlacklistScope
import io.github.wykopmobilny.ui.base.ItemState
import io.github.wykopmobilny.ui.base.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@BlacklistScope
internal class BlacklistViewStateStorage
    @Inject
    constructor() {
        val state = MutableStateFlow(value = BlacklistViewState())

        fun update(updater: (BlacklistViewState) -> BlacklistViewState) {
            state.update(updater)
        }

        // Atomowo zaznacza rozpoczecie ladowania; zwraca true tylko dla pierwszego
        // wywolania (kolejne kolektory nie odpalaja ponownie poboru z API).
        fun markLoadStartedIfNeeded(): Boolean {
            while (true) {
                val current = state.value
                if (current.loadStarted) return false
                if (state.compareAndSet(current, current.copy(loadStarted = true))) return true
            }
        }
    }

data class BlacklistViewState(
    val generalResource: Resource = Resource.idle(),
    val loadStarted: Boolean = false,
    val users: PageState = PageState(),
    val domains: PageState = PageState(),
    val tags: PageState = PageState(),
) {
    data class PageState(
        val loading: Boolean = true,
        val items: List<String> = emptyList(),
        val itemStates: Map<String, ItemState> = emptyMap(),
        val addInProgress: Boolean = false,
    )
}

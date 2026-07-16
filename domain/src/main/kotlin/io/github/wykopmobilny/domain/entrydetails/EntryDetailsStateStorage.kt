package io.github.wykopmobilny.domain.entrydetails

import io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsScope
import io.github.wykopmobilny.ui.base.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Źródło prawdy ekranu wpisu - scope'owany stan w pamięci (wzorzec
 * LinkDetailsViewStateStorage). Świadomie bez Store5: komentarze wpisu nie mają
 * tabel w cache, a dwukierunkowy pager i tak omijałby fetcher (reguła jednej
 * emisji z flowSourceOfTruth) dla wszystkiego poza pierwszą stroną.
 */
@EntryDetailsScope
internal class EntryDetailsStateStorage
    @Inject
    constructor() {
        val state = MutableStateFlow(value = EntryDetailsState())

        fun update(updater: (EntryDetailsState) -> EntryDetailsState) {
            state.update(updater)
        }
    }

internal data class EntryDetailsState(
    val entry: EntryResponseV3? = null,
    val comments: List<EntryCommentResponseV3> = emptyList(),
    // Zakres załadowanych stron; 0 = nic nie załadowano.
    val oldestLoadedPage: Int = 0,
    val newestLoadedPage: Int = 0,
    // Czy za newestLoadedPage jest kolejna strona (pełna strona = może być więcej).
    val hasNewer: Boolean = false,
    val totalCount: Int? = null,
    val perPage: Int? = null,
    val isLoadingOlder: Boolean = false,
    val isLoadingNewer: Boolean = false,
    val generalResource: Resource = Resource.idle(),
) {
    val hasOlder: Boolean get() = oldestLoadedPage > 1

    // Ostatnia strona liczona po stronie klienta - API zwraca tylko total i per_page.
    val lastPage: Int?
        get() {
            val total = totalCount ?: return null
            val size = perPage?.takeIf { it > 0 } ?: return null
            if (total <= 0) return null
            return (total + size - 1) / size
        }
}

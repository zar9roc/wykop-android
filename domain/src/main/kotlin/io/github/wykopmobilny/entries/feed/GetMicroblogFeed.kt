package io.github.wykopmobilny.entries.feed

import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.ui.base.Query
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.base.components.SwipeRefreshUi

interface GetMicroblogFeed : Query<MicroblogFeedUi>

/**
 * Stan feedu mikrobloga (Gorące). Jak w entrydetails: niosemy surowe modele v3,
 * a mapowanie na legacy Entry + renderowanie (EntryViewHolder) zostaje po stronie
 * aplikacji - świadomy kompromis portu (reużycie sprawdzonych viewholderów).
 */
data class MicroblogFeedUi(
    val entries: List<EntryResponseV3>,
    val sort: MicroblogFeedSort,
    val isInitialLoading: Boolean,
    val hasMore: Boolean,
    val isLoadingMore: Boolean,
    val loadMoreAction: () -> Unit,
    val selectSortAction: (MicroblogFeedSort) -> Unit,
    val swipeRefresh: SwipeRefreshUi,
    val errorDialog: ErrorDialogUi?,
)

// Tryby feedu Gorące (mapowane na /v3/entries sort + last_update w pagerze).
enum class MicroblogFeedSort {
    HOT_24,
    HOT_12,
    HOT_6,
    HOT_2,
    ACTIVE,
    NEWEST,
}

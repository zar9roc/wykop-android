package io.github.wykopmobilny.entries.details

import io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.ui.base.Query
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.base.components.SwipeRefreshUi

interface GetEntryDetails : Query<EntryDetailsUi>

/**
 * Stan ekranu wpisu (V2). Świadomy kompromis portu: dane treści to surowe modele
 * odpowiedzi v3 (wspólne dla domain i app) - mapowanie na modele widoków oraz
 * akcje na treści (głosy, ulubione itd.) zostają po stronie aplikacji, gdzie
 * viewholdery wpisu mają już cały komplet zachowań. Domain jest właścicielem
 * cyklu życia, paginacji i odświeżania.
 */
data class EntryDetailsUi(
    val entry: EntryResponseV3?,
    val comments: List<EntryCommentResponseV3>,
    val totalCommentsCount: Int?,
    val isInitialLoading: Boolean,
    val hasOlder: Boolean,
    val hasNewer: Boolean,
    val isLoadingOlder: Boolean,
    val isLoadingNewer: Boolean,
    val loadOlderAction: () -> Unit,
    val loadNewerAction: () -> Unit,
    // Ostatnia strona (ceil(total/per_page)) - do komunikatu przy skoku.
    val lastPage: Int?,
    // null = brak paginacji (jedna strona) - przycisk skoku ukryty.
    val jumpToNewestAction: (() -> Unit)?,
    val swipeRefresh: SwipeRefreshUi,
    val errorDialog: ErrorDialogUi?,
)

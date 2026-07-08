package io.github.wykopmobilny.ui.blacklist

import io.github.wykopmobilny.ui.base.Query
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi

interface GetBlacklistDetails : Query<BlacklistedDetailsUi>

class BlacklistedDetailsUi(
    val errorDialog: ErrorDialogUi?,
    val users: ElementPage,
    val domains: ElementPage,
    val tags: ElementPage,
) {
    class ElementPage(
        val isLoading: Boolean,
        val elements: List<BlacklistedElementUi>,
        val add: AddUi,
        val refreshAction: () -> Unit,
    )

    class AddUi(
        val inProgress: Boolean,
        // null => brak autopodpowiedzi (domeny wpisywane recznie w polu tekstowym)
        val suggestions: (suspend (String) -> List<String>)?,
        val submit: (String) -> Unit,
    )
}

class BlacklistedElementUi(
    val name: String,
    val state: StateUi,
) {
    sealed class StateUi {
        data class Default(
            val unblock: () -> Unit,
        ) : StateUi()

        object InProgress : StateUi()

        data class Error(
            val showError: () -> Unit,
        ) : StateUi()
    }
}

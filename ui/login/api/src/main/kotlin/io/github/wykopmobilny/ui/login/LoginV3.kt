package io.github.wykopmobilny.ui.login

import io.github.wykopmobilny.ui.base.Query
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi

interface LoginV3 : Query<LoginV3Ui> {
    fun login()

    fun clearError()
}

data class LoginV3Ui(
    val isLoading: Boolean,
    val errorDialog: ErrorDialogUi?,
    val isLoggedIn: Boolean,
    val connectUrl: String? = null,
    val parseUrlAction: (String) -> Unit = {},
    val isCallbackUrl: (String) -> Boolean = { false },
)

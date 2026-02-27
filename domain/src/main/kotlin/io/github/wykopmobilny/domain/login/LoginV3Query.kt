package io.github.wykopmobilny.domain.login

import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.fresh
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.domain.login.di.LoginScope
import io.github.wykopmobilny.domain.navigation.AppRestarter
import io.github.wykopmobilny.domain.utils.safe
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.storage.api.Blacklist
import io.github.wykopmobilny.storage.api.JwtToken
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.storage.api.LoggedUserInfo
import io.github.wykopmobilny.storage.api.UserSession
import io.github.wykopmobilny.ui.base.FailedAction
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.login.LoginV3
import io.github.wykopmobilny.ui.login.LoginV3Ui
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LoginV3Query
    @Inject
    constructor(
        private val authV3Api: AuthV3RetrofitApi,
        private val jwtTokenStorage: JwtTokenStorage,
        private val userInfoStore: Store<UserSession, LoggedUserInfo>,
        private val blacklistStore: Store<Unit, Blacklist>,
        private val appRestarter: AppRestarter,
        private val viewStateStorage: SimpleViewStateStorage,
        private val appScopes: AppScopes,
    ) : LoginV3 {
        override fun invoke() =
            viewStateStorage.state.map { viewState ->
                LoginV3Ui(
                    isLoading = viewState.isLoading,
                    errorDialog =
                        viewState.failedAction?.let { failure ->
                            ErrorDialogUi(
                                error = failure.cause,
                                retryAction = failure.retryAction,
                                dismissAction = { appScopes.safe<LoginScope> { viewStateStorage.update { it.copy(failedAction = null) } } },
                            )
                        },
                    isLoggedIn = false,
                )
            }

        override fun login(
            username: String,
            password: String,
        ) = appScopes.safe<LoginScope> {
            viewStateStorage.update { it.copy(isLoading = true) }

            runCatching {
                // Call JWT auth endpoint
                val response =
                    authV3Api.authenticate(
                        AuthRequestV3(username = username, password = password),
                    )

                val authData = response.data
                if (authData != null) {
                    // Save JWT token
                    val expiresAt = System.currentTimeMillis() + (authData.expiresIn * 1000)
                    jwtTokenStorage.updateJwtToken(
                        JwtToken(
                            accessToken = authData.token,
                            refreshToken = authData.refreshToken,
                            expiresAt = expiresAt,
                        ),
                    )

                    // Fetch user info and blacklist
                    val userSession = UserSession(username, "") // JWT doesn't use token from session
                    userInfoStore.fresh(userSession)
                    blacklistStore.fresh(Unit)

                    // Restart app to apply login
                    appRestarter.restart()
                } else {
                    throw IllegalStateException(response.error?.messagePl ?: "Unknown error")
                }
            }.onFailure { throwable ->
                viewStateStorage.update {
                    it.copy(isLoading = false, failedAction = FailedAction(cause = throwable, retryAction = null))
                }
            }.onSuccess {
                viewStateStorage.update { it.copy(isLoading = false, failedAction = null) }
            }
        }

        override fun clearError() =
            appScopes.safe<LoginScope> {
                viewStateStorage.update { it.copy(failedAction = null) }
            }
    }

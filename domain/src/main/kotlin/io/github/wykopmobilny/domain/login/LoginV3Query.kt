package io.github.wykopmobilny.domain.login

import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.domain.login.di.LoginScope
import io.github.wykopmobilny.domain.startup.AppConfig
import io.github.wykopmobilny.domain.utils.safe
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import io.github.wykopmobilny.ui.base.FailedAction
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.login.LoginV3
import io.github.wykopmobilny.ui.login.LoginV3Ui
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class LoginV3Query
    @Inject
    constructor(
        private val authV3Api: AuthV3RetrofitApi,
        private val bearerTokenStorage: BearerTokenStorage,
        private val viewStateStorage: SimpleViewStateStorage,
        private val appScopes: AppScopes,
        private val appConfig: AppConfig,
    ) : LoginV3 {
        private val connectUrlState = MutableStateFlow<String?>(null)

        override fun invoke() =
            combine(viewStateStorage.state, connectUrlState) { viewState, connectUrl ->
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
                    connectUrl = connectUrl,
                )
            }

        override fun login(
            username: String,
            password: String,
        ) = appScopes.safe<LoginScope> {
            viewStateStorage.update { it.copy(isLoading = true) }
            connectUrlState.value = null

            runCatching {
                // Step 1: POST /v3/auth to get app-level bearer token
                val authResponse =
                    authV3Api.authenticate(
                        AuthRequestV3(key = appConfig.v3ApiKey, secret = appConfig.v3ApiSecret),
                    )

                val authData = authResponse.data
                if (authData == null) {
                    throw IllegalStateException(authResponse.error?.messagePl ?: "Failed to authenticate app")
                }

                // Step 2: Save bearer token to BearerTokenStorage (used by BearerAuthInterceptor for /v3/connect)
                bearerTokenStorage.updateBearerToken(authData.token)

                // Step 3: GET /v3/connect to get connectUrl for WebView
                val connectResponse = authV3Api.connect()

                val connectData = connectResponse.data
                if (connectData == null) {
                    throw IllegalStateException(connectResponse.error?.messagePl ?: "Failed to get connect URL")
                }

                // Step 4: Set connectUrl for UI to open WebView
                connectUrlState.value = connectData.connectUrl

                // TODO: Faza 3.x - WebView callback handling
                // After user logs in via WebView, we need to:
                // 1. Parse JWT token from callback URL
                // 2. Save JWT token to JwtTokenStorage
                // 3. Fetch user info and blacklist
                // 4. Restart app to apply login
                // For now, this is just the first part of the auth flow.
            }.onFailure { throwable ->
                viewStateStorage.update {
                    it.copy(isLoading = false, failedAction = FailedAction(cause = throwable, retryAction = null))
                }
                connectUrlState.value = null
            }.onSuccess {
                viewStateStorage.update { it.copy(isLoading = false, failedAction = null) }
            }
        }

        override fun clearError() =
            appScopes.safe<LoginScope> {
                viewStateStorage.update { it.copy(failedAction = null) }
            }
    }

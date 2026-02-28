package io.github.wykopmobilny.domain.login

import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.fresh
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.domain.login.di.LoginScope
import io.github.wykopmobilny.domain.navigation.AppRestarter
import io.github.wykopmobilny.domain.startup.AppConfig
import io.github.wykopmobilny.domain.utils.safe
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import io.github.wykopmobilny.storage.api.Blacklist
import io.github.wykopmobilny.storage.api.LoggedUserInfo
import io.github.wykopmobilny.storage.api.SessionStorage
import io.github.wykopmobilny.storage.api.UserSession
import io.github.wykopmobilny.ui.base.FailedAction
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.login.LoginV3
import io.github.wykopmobilny.ui.login.LoginV3Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginV3Query
    @Inject
    constructor(
        private val authV3Api: AuthV3RetrofitApi,
        private val bearerTokenStorage: BearerTokenStorage,
        private val sessionStorage: SessionStorage,
        private val userInfoStore: Store<UserSession, LoggedUserInfo>,
        private val blacklistStore: Store<Unit, Blacklist>,
        private val appRestarter: AppRestarter,
        private val viewStateStorage: SimpleViewStateStorage,
        private val appScopes: AppScopes,
        private val appConfig: AppConfig,
    ) : LoginV3 {
        private val connectUrlState = MutableStateFlow<String?>(null)
        private var currentUsername: String? = null

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
                    parseUrlAction = ::onUrlInvoked,
                )
            }

        override fun login(
            username: String,
            password: String,
        ) = appScopes.safe<LoginScope> {
            currentUsername = username
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
            }.onFailure { throwable ->
                viewStateStorage.update {
                    it.copy(isLoading = false, failedAction = FailedAction(cause = throwable, retryAction = null))
                }
                connectUrlState.value = null
            }.onSuccess {
                viewStateStorage.update { it.copy(isLoading = false, failedAction = null) }
            }
        }

        private fun onUrlInvoked(url: String) =
            appScopes.safe<LoginScope> {
                val username = currentUsername ?: return@safe
                val userSession =
                    withContext(Dispatchers.Default) {
                        val match = connectCallbackPattern.find(url) ?: return@withContext null

                        val token = match.groups[1]?.value?.takeIf { it.isNotBlank() }
                        val refreshToken = match.groups[2]?.value?.takeIf { it.isNotBlank() }

                        if (token.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                            null
                        } else {
                            // TODO: Save refreshToken to JwtTokenStorage in future phase
                            UserSession(username, token)
                        }
                    } ?: return@safe
                viewStateStorage.update { it.copy(isLoading = true) }
                connectUrlState.value = null

                runCatching {
                    sessionStorage.updateSession(userSession)
                    userInfoStore.fresh(userSession)
                    blacklistStore.fresh(Unit)
                    appRestarter.restart()
                }.onFailure { throwable ->
                    sessionStorage.updateSession(null)
                    userInfoStore.clearAll()
                    blacklistStore.clearAll()
                    viewStateStorage.update { it.copy(isLoading = false, failedAction = FailedAction(cause = throwable)) }
                }.onSuccess {
                    viewStateStorage.update { it.copy(isLoading = false, failedAction = null) }
                }
            }

        override fun clearError() =
            appScopes.safe<LoginScope> {
                viewStateStorage.update { it.copy(failedAction = null) }
            }

        companion object {
            // API v3 callback format: https://wykop.pl/?token={JWT}&rtoken={REFRESH_TOKEN}
            private val connectCallbackPattern =
                "[?]token=([^&]+)&rtoken=([^&]+)".toRegex()
        }
    }

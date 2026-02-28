package io.github.wykopmobilny.domain.login

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.domain.login.di.LoginScope
import io.github.wykopmobilny.domain.navigation.AppRestarter
import io.github.wykopmobilny.domain.utils.safe
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.storage.api.JwtToken
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.ui.base.FailedAction
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.login.LoginV3
import io.github.wykopmobilny.ui.login.LoginV3Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.inject.Inject

class LoginV3Query
    @Inject
    constructor(
        private val authV3Api: AuthV3RetrofitApi,
        private val jwtTokenStorage: JwtTokenStorage,
        private val appRestarter: AppRestarter,
        private val viewStateStorage: SimpleViewStateStorage,
        private val appScopes: AppScopes,
    ) : LoginV3 {
        private val connectUrlState = MutableStateFlow<String?>(null)
        private var isLoginFlowActive = false

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
                    isCallbackUrl = { url -> connectCallbackPattern.containsMatchIn(url) },
                )
            }

        override fun login() =
            appScopes.safe<LoginScope> {
                Napier.i(tag = TAG) { "login: Starting login flow" }
                isLoginFlowActive = true
                viewStateStorage.update { it.copy(isLoading = true) }
                connectUrlState.value = null

                runCatching {
                    // Bearer token is already available from app startup (InitializeApp).
                    // GET /v3/connect to get connectUrl for WebView
                    Napier.d(tag = TAG) { "login: Calling authV3Api.connect()" }
                    val connectResponse = authV3Api.connect()
                    Napier.d(
                        tag = TAG,
                    ) { "login: Received connect response: data=${connectResponse.data != null}, error=${connectResponse.error}" }

                    val connectData = connectResponse.data
                    if (connectData == null) {
                        val errorMsg = connectResponse.error?.messagePl ?: "Failed to get connect URL"
                        Napier.e(tag = TAG) { "login: Connect data is null: $errorMsg" }
                        throw IllegalStateException(errorMsg)
                    }

                    Napier.i(tag = TAG) { "login: Setting connectUrl: ${connectData.connectUrl}" }
                    connectUrlState.value = connectData.connectUrl
                }.onFailure { throwable ->
                    Napier.e(tag = TAG, throwable = throwable) { "login: Failed to get connect URL" }
                    isLoginFlowActive = false
                    viewStateStorage.update {
                        it.copy(isLoading = false, failedAction = FailedAction(cause = throwable, retryAction = null))
                    }
                    connectUrlState.value = null
                }.onSuccess {
                    Napier.i(tag = TAG) { "login: Connect URL obtained successfully" }
                    viewStateStorage.update { it.copy(isLoading = false, failedAction = null) }
                }
            }

        private fun onUrlInvoked(url: String) =
            appScopes.safe<LoginScope> {
                Napier.i(tag = TAG) { "onUrlInvoked: Processing callback URL" }
                if (!isLoginFlowActive) {
                    Napier.w(tag = TAG) { "onUrlInvoked: Login flow is not active, ignoring callback" }
                    return@safe
                }
                Napier.d(tag = TAG) { "onUrlInvoked: Parsing credentials from URL" }
                val credentials =
                    withContext(Dispatchers.Default) {
                        val match = connectCallbackPattern.find(url)
                        if (match == null) {
                            Napier.w(tag = TAG) { "onUrlInvoked: URL does not match callback pattern" }
                            return@withContext null
                        }

                        val token = match.groups[1]?.value?.takeIf { it.isNotBlank() }
                        val refreshToken = match.groups[2]?.value?.takeIf { it.isNotBlank() }

                        if (token.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                            Napier.e(tag = TAG) { "onUrlInvoked: Token or refreshToken is blank" }
                            null
                        } else {
                            Napier.d(tag = TAG) { "onUrlInvoked: Token extracted, decoding JWT expiration" }
                            // Decode JWT expiration timestamp
                            val expiresAt = decodeJwtExpiration(token)
                            Napier.d(tag = TAG) { "onUrlInvoked: JWT expiration decoded: $expiresAt" }
                            Triple(token, refreshToken, expiresAt)
                        }
                    }
                if (credentials == null) {
                    Napier.e(tag = TAG) { "onUrlInvoked: Failed to extract credentials from URL" }
                    return@safe
                }
                Napier.i(tag = TAG) { "onUrlInvoked: Credentials extracted successfully, saving to storage" }
                viewStateStorage.update { it.copy(isLoading = true) }

                val (token, refreshToken, expiresAt) = credentials

                runCatching {
                    Napier.d(tag = TAG) { "onUrlInvoked: Updating JWT token in storage" }
                    // Save JWT token
                    jwtTokenStorage.updateJwtToken(
                        JwtToken(
                            accessToken = token,
                            refreshToken = refreshToken,
                            expiresAt = expiresAt,
                        ),
                    )

                    Napier.d(tag = TAG) { "onUrlInvoked: Waiting for token to be available in Flow" }
                    // Wait for token to be available in Flow (DataStore is async)
                    jwtTokenStorage.jwtToken.first { it != null }

                    Napier.i(tag = TAG) { "onUrlInvoked: Token saved, restarting app" }
                    appRestarter.restart()
                }.onFailure { throwable ->
                    Napier.e(tag = TAG, throwable = throwable) { "onUrlInvoked: Failed to save token or restart app" }
                    jwtTokenStorage.updateJwtToken(null)
                    connectUrlState.value = null
                    viewStateStorage.update { it.copy(isLoading = false, failedAction = FailedAction(cause = throwable)) }
                }.onSuccess {
                    Napier.i(tag = TAG) { "onUrlInvoked: Login completed successfully" }
                    connectUrlState.value = null
                    viewStateStorage.update { it.copy(isLoading = false, failedAction = null) }
                }
            }

        override fun clearError() =
            appScopes.safe<LoginScope> {
                viewStateStorage.update { it.copy(failedAction = null) }
            }

        companion object {
            private const val TAG = "LoginV3Query"

            // API v3 callback format: https://wykop.pl/?token={JWT}&rtoken={REFRESH_TOKEN}
            private val connectCallbackPattern =
                "[?]token=([^&]+)&rtoken=([^&]+)".toRegex()

            /**
             * Decodes JWT token and extracts expiration timestamp.
             * JWT format: header.payload.signature (all base64-encoded)
             * Payload contains "exp" claim as Unix timestamp in seconds.
             *
             * @param token JWT access token
             * @return Expiration timestamp in milliseconds (System.currentTimeMillis() format)
             * @throws IllegalArgumentException if JWT is malformed or missing exp claim
             */
            private fun decodeJwtExpiration(token: String): Long {
                val parts = token.split(".")
                require(parts.size == 3) { "Invalid JWT format: expected 3 parts separated by dots" }

                // Decode payload (second part) using Java Base64 decoder
                val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))

                // Extract exp claim using regex (avoiding JSONObject dependency)
                val expMatch = """"exp"\s*:\s*(\d+)""".toRegex().find(payloadJson)
                val expSeconds =
                    expMatch
                        ?.groups
                        ?.get(1)
                        ?.value
                        ?.toLongOrNull()
                        ?: throw IllegalArgumentException("JWT missing or invalid 'exp' claim")

                // Convert from seconds to milliseconds
                return expSeconds * 1000
            }
        }
    }

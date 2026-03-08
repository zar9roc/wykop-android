package io.github.wykopmobilny.wykop.remote

import dagger.Lazy
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.RefreshTokenRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.storage.api.JwtToken
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import java.io.IOException
import java.util.Base64
import javax.inject.Inject

/**
 * Helper class for refreshing JWT tokens.
 * Shared logic used by both TokenRefreshAuthenticator (401) and Forbidden403RetryInterceptor (403).
 */
internal class TokenRefreshHelper
    @Inject
    constructor(
        private val authApiLazy: Lazy<AuthV3RetrofitApi>,
        private val jwtTokenStorage: JwtTokenStorage,
    ) {
        /**
         * Attempts to refresh the JWT token using the refresh token.
         * @return New JwtToken if successful, null if refresh failed
         */
        fun refreshToken(currentToken: JwtToken): JwtToken? {
            Napier.d("TokenRefreshHelper - Attempting to refresh JWT token", tag = "TokenRefreshHelper")

            // Attempt to refresh the token
            val newTokenResponse =
                runBlocking {
                    try {
                        authApiLazy.get().refreshToken(
                            WykopApiRequestV3(
                                data = RefreshTokenRequestV3(refreshToken = currentToken.refreshToken),
                            ),
                        )
                    } catch (e: HttpException) {
                        Napier.w(
                            "TokenRefreshHelper - JWT refresh failed with HTTP ${e.code()}: ${e.message()}",
                            e,
                            tag = "TokenRefreshHelper",
                        )
                        // Refresh failed, clear tokens (user needs to re-login)
                        jwtTokenStorage.updateJwtToken(null)
                        return@runBlocking null
                    } catch (e: IOException) {
                        Napier.w("TokenRefreshHelper - JWT refresh failed due to network error", e, tag = "TokenRefreshHelper")
                        // Network error, clear tokens (user needs to re-login)
                        jwtTokenStorage.updateJwtToken(null)
                        return@runBlocking null
                    }
                }

            val newAuthData = newTokenResponse?.data ?: return null

            // Decode expiration from JWT token payload
            val expiresAt = decodeJwtExpiration(newAuthData.token)

            // Create new token
            val newToken =
                JwtToken(
                    accessToken = newAuthData.token,
                    refreshToken = newAuthData.refreshToken,
                    expiresAt = expiresAt,
                )

            // Save new tokens
            runBlocking {
                jwtTokenStorage.updateJwtToken(newToken)
            }

            Napier.d("TokenRefreshHelper - JWT token refreshed successfully", tag = "TokenRefreshHelper")

            return newToken
        }

        /**
         * Gets the current JWT token from storage synchronously.
         * @return Current JwtToken or null if not available
         */
        fun getCurrentToken(): JwtToken? =
            runBlocking {
                jwtTokenStorage.jwtToken.first()
            }

        private fun decodeJwtExpiration(token: String): Long {
            val parts = token.split(".")
            if (parts.size != 3) return 0L

            val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
            val expMatch = """"exp"\s*:\s*(\d+)""".toRegex().find(payloadJson)
            val expSeconds =
                expMatch
                    ?.groups
                    ?.get(1)
                    ?.value
                    ?.toLongOrNull() ?: return 0L

            return expSeconds * 1000
        }
    }

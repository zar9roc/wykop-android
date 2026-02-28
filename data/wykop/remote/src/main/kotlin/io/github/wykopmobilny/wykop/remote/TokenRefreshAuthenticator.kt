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
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException
import java.util.Base64
import javax.inject.Inject

internal class TokenRefreshAuthenticator
    @Inject
    constructor(
        private val authApiLazy: Lazy<AuthV3RetrofitApi>,
        private val jwtTokenStorage: JwtTokenStorage,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            val path = response.request.url.encodedPath
            val url = response.request.url.toString()

            Napier.d("TokenRefreshAuthenticator - 401 response for URL: $url", tag = "TokenRefreshAuthenticator")
            Napier.d("TokenRefreshAuthenticator - Path: $path", tag = "TokenRefreshAuthenticator")

            // Only handle 401 for v3 API endpoints (except auth and connect endpoints)
            if (!path.startsWith("/api/v3/") || path.startsWith("/api/v3/auth") || path == "/api/v3/connect") {
                Napier.d("TokenRefreshAuthenticator - Skipping: not v3 API or auth/connect endpoint", tag = "TokenRefreshAuthenticator")
                return null
            }

            Napier.d("TokenRefreshAuthenticator - Attempting to refresh JWT token", tag = "TokenRefreshAuthenticator")

            // Get current JWT token
            val currentToken =
                runBlocking {
                    jwtTokenStorage.jwtToken.first()
                } ?: return null

            // Check if we already tried to refresh (avoid infinite loop)
            val authorizationHeader = response.request.header("Authorization")
            if (authorizationHeader != null && authorizationHeader.contains(currentToken.accessToken)) {
                // This request already used the current token, so it's already refreshed or invalid
                synchronized(this) {
                    // Double-check: maybe another thread already refreshed the token
                    val latestToken =
                        runBlocking {
                            jwtTokenStorage.jwtToken.first()
                        }

                    if (latestToken != null && latestToken.accessToken != currentToken.accessToken) {
                        // Token was refreshed by another thread, retry with new token
                        return response.request
                            .newBuilder()
                            .header("Authorization", "Bearer ${latestToken.accessToken}")
                            .build()
                    }
                }
            }

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
                        Napier.w("JWT refresh failed with HTTP ${e.code()}: ${e.message()}", e)
                        // Refresh failed, clear tokens (user needs to re-login)
                        jwtTokenStorage.updateJwtToken(null)
                        return@runBlocking null
                    } catch (e: IOException) {
                        Napier.w("JWT refresh failed due to network error", e)
                        // Network error, clear tokens (user needs to re-login)
                        jwtTokenStorage.updateJwtToken(null)
                        return@runBlocking null
                    }
                }

            val newAuthData = newTokenResponse?.data ?: return null

            // Decode expiration from JWT token payload
            val expiresAt = decodeJwtExpiration(newAuthData.token)

            // Save new tokens
            val newToken =
                JwtToken(
                    accessToken = newAuthData.token,
                    refreshToken = newAuthData.refreshToken,
                    expiresAt = expiresAt,
                )

            runBlocking {
                jwtTokenStorage.updateJwtToken(newToken)
            }

            Napier.d("TokenRefreshAuthenticator - JWT token refreshed successfully", tag = "TokenRefreshAuthenticator")

            // Retry the original request with new token
            return response.request
                .newBuilder()
                .header("Authorization", "Bearer ${newToken.accessToken}")
                .build()
        }

        private fun decodeJwtExpiration(token: String): Long {
            val parts = token.split(".")
            if (parts.size != 3) return 0L

            val payloadJson = String(Base64.getUrlDecoder().decode(parts[1]))
            val expMatch = """"exp"\s*:\s*(\d+)""".toRegex().find(payloadJson)
            val expSeconds = expMatch?.groups?.get(1)?.value?.toLongOrNull() ?: return 0L

            return expSeconds * 1000
        }
    }

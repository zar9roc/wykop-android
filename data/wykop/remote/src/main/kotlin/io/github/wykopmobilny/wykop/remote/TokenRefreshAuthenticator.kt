package io.github.wykopmobilny.wykop.remote

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

internal class TokenRefreshAuthenticator
    @Inject
    constructor(
        private val tokenRefreshHelper: TokenRefreshHelper,
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
            val currentToken = tokenRefreshHelper.getCurrentToken() ?: return null

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

            // Attempt to refresh the token using helper
            val newToken = tokenRefreshHelper.refreshToken(currentToken) ?: return null

            // Retry the original request with new token
            return response.request
                .newBuilder()
                .header("Authorization", "Bearer ${newToken.accessToken}")
                .build()
        }
    }

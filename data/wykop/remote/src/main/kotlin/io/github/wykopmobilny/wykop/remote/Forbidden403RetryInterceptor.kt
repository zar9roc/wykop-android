package io.github.wykopmobilny.wykop.remote

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that handles 403 Forbidden responses by attempting to refresh the JWT token.
 * Works in conjunction with TokenRefreshAuthenticator (which handles 401 responses).
 *
 * When a 403 response is received:
 * 1. Checks if it's a v3 API endpoint (excludes auth/connect endpoints)
 * 2. Attempts to refresh the JWT token using the refresh token
 * 3. Retries the original request with the new access token
 * 4. If refresh fails, clears tokens and returns the original 403 response
 */
internal class Forbidden403RetryInterceptor
    @Inject
    constructor(
        private val tokenRefreshHelper: TokenRefreshHelper,
        private val jwtTokenStorage: JwtTokenStorage,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val response = chain.proceed(request)

            // Only handle 403 responses
            if (response.code != 403) {
                return response
            }

            val path = request.url.encodedPath
            val url = request.url.toString()

            Napier.d("Forbidden403RetryInterceptor - 403 response for URL: $url", tag = "Forbidden403RetryInterceptor")
            Napier.d("Forbidden403RetryInterceptor - Path: $path", tag = "Forbidden403RetryInterceptor")

            // Only handle 403 for v3 API endpoints (except auth and connect endpoints)
            if (!path.startsWith("/api/v3/") || path.startsWith("/api/v3/auth") ||
                path == "/api/v3/connect" || path == "/api/v3/refresh-token"
            ) {
                Napier.d(
                    "Forbidden403RetryInterceptor - Skipping: not v3 API or auth/connect endpoint",
                    tag = "Forbidden403RetryInterceptor",
                )
                return response
            }

            // Get current JWT token
            val currentToken = tokenRefreshHelper.getCurrentToken()
            if (currentToken == null) {
                Napier.w("Forbidden403RetryInterceptor - No JWT token available", tag = "Forbidden403RetryInterceptor")
                return response
            }

            // Check if we already tried to refresh (avoid infinite loop)
            val authorizationHeader = request.header("Authorization")
            if (authorizationHeader != null && authorizationHeader.contains(currentToken.accessToken)) {
                // This request already used the current token, check if another thread refreshed it
                synchronized(this) {
                    val latestToken =
                        runBlocking {
                            jwtTokenStorage.jwtToken.first()
                        }

                    if (latestToken != null && latestToken.accessToken != currentToken.accessToken) {
                        // Token was refreshed by another thread, retry with new token
                        Napier.d(
                            "Forbidden403RetryInterceptor - Token already refreshed by another thread, retrying",
                            tag = "Forbidden403RetryInterceptor",
                        )
                        response.close()
                        val newRequest =
                            request
                                .newBuilder()
                                .header("Authorization", "Bearer ${latestToken.accessToken}")
                                .build()
                        return chain.proceed(newRequest)
                    }
                }
            }

            Napier.d("Forbidden403RetryInterceptor - Attempting to refresh JWT token", tag = "Forbidden403RetryInterceptor")

            // Attempt to refresh the token using helper
            val newToken = tokenRefreshHelper.refreshToken(currentToken)
            if (newToken == null) {
                Napier.w(
                    "Forbidden403RetryInterceptor - Token refresh failed, returning original 403 response",
                    tag = "Forbidden403RetryInterceptor",
                )
                return response
            }

            Napier.d("Forbidden403RetryInterceptor - Token refreshed, retrying request", tag = "Forbidden403RetryInterceptor")

            // Close original response and retry with new token
            response.close()
            val newRequest =
                request
                    .newBuilder()
                    .header("Authorization", "Bearer ${newToken.accessToken}")
                    .build()

            return chain.proceed(newRequest)
        }
    }

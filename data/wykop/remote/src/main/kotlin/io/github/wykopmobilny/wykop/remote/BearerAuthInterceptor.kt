package io.github.wykopmobilny.wykop.remote

import io.github.wykopmobilny.storage.api.BearerTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that adds app-level bearer token to all /v3/ requests.
 * Bearer token is obtained at app startup via POST /v3/auth.
 * For logged-in users, JwtAuthInterceptor will override this header with the JWT token.
 */
internal class BearerAuthInterceptor
    @Inject
    constructor(
        private val bearerTokenStorage: BearerTokenStorage,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath

            // Only add bearer token for v3 API endpoints (skip /v3/auth where we obtain it)
            if (!path.startsWith("/v3/") || path.startsWith("/v3/auth")) {
                return chain.proceed(request)
            }

            // Get bearer token synchronously
            val bearerToken =
                runBlocking {
                    bearerTokenStorage.bearerToken.first()
                }

            // If no token, proceed without Authorization header
            if (bearerToken == null) {
                return chain.proceed(request)
            }

            // Add Authorization header with Bearer token
            val newRequest =
                request
                    .newBuilder()
                    .header("Authorization", "Bearer $bearerToken")
                    .build()

            return chain.proceed(newRequest)
        }
    }

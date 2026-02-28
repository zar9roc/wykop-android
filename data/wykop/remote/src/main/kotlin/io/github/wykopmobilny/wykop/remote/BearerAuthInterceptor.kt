package io.github.wykopmobilny.wykop.remote

import io.github.aakira.napier.Napier
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
            val url = request.url.toString()

            Napier.d("BearerAuthInterceptor - URL: $url", tag = "BearerAuthInterceptor")
            Napier.d("BearerAuthInterceptor - Path: $path", tag = "BearerAuthInterceptor")

            // Only add bearer token for v3 API endpoints (skip /v3/auth where we obtain it)
            if (!path.startsWith("/api/v3/") || path.startsWith("/api/v3/auth")) {
                Napier.d("BearerAuthInterceptor - Skipping: not v3 API or auth endpoint", tag = "BearerAuthInterceptor")
                return chain.proceed(request)
            }

            // Get bearer token synchronously
            val bearerToken =
                runBlocking {
                    bearerTokenStorage.bearerToken.first()
                }

            // If no token, proceed without Authorization header
            if (bearerToken == null) {
                Napier.w("BearerAuthInterceptor - No bearer token available", tag = "BearerAuthInterceptor")
                return chain.proceed(request)
            }

            Napier.d("BearerAuthInterceptor - Adding bearer token: Bearer ${bearerToken.take(20)}...", tag = "BearerAuthInterceptor")

            // Add Authorization header with Bearer token
            val newRequest =
                request
                    .newBuilder()
                    .header("Authorization", "Bearer $bearerToken")
                    .build()

            return chain.proceed(newRequest)
        }
    }

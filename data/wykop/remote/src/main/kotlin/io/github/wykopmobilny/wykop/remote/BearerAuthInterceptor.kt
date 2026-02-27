package io.github.wykopmobilny.wykop.remote

import io.github.wykopmobilny.storage.api.BearerTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that adds app-level bearer token to /v3/connect requests.
 * Bearer token is obtained from app-level authentication (POST /v3/auth with apiKey/apiSecret).
 */
internal class BearerAuthInterceptor
    @Inject
    constructor(
        private val bearerTokenStorage: BearerTokenStorage,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath

            // Only add bearer token for /v3/connect endpoint
            if (path != "/v3/connect") {
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
                    .addHeader("Authorization", "Bearer $bearerToken")
                    .build()

            return chain.proceed(newRequest)
        }
    }

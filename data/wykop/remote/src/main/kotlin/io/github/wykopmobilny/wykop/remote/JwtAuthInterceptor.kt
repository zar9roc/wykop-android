package io.github.wykopmobilny.wykop.remote

import io.github.wykopmobilny.storage.api.JwtTokenStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

internal class JwtAuthInterceptor
    @Inject
    constructor(
        private val jwtTokenStorage: JwtTokenStorage,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath

            // Only add JWT token for v3 API endpoints
            if (!path.startsWith("/v3/")) {
                return chain.proceed(request)
            }

            // Skip JWT for auth endpoints
            if (path.startsWith("/v3/auth")) {
                return chain.proceed(request)
            }

            // Get JWT token synchronously
            val jwtToken =
                runBlocking {
                    jwtTokenStorage.jwtToken.first()
                }

            // If no token, proceed without Authorization header
            if (jwtToken == null) {
                return chain.proceed(request)
            }

            // Add Authorization header with Bearer token
            val newRequest =
                request
                    .newBuilder()
                    .addHeader("Authorization", "Bearer ${jwtToken.accessToken}")
                    .build()

            return chain.proceed(newRequest)
        }
    }

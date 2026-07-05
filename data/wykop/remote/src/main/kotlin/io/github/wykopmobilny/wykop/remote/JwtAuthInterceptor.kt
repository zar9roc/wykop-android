package io.github.wykopmobilny.wykop.remote

import io.github.aakira.napier.Napier
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
            val url = request.url.toString()

            Napier.d("JwtAuthInterceptor - URL: $url", tag = "JwtAuthInterceptor")
            Napier.d("JwtAuthInterceptor - Path: $path", tag = "JwtAuthInterceptor")

            // Only add JWT token for v3 API endpoints
            if (!path.startsWith("/api/v3/")) {
                Napier.d("JwtAuthInterceptor - Skipping: path does not start with '/api/v3/'", tag = "JwtAuthInterceptor")
                return chain.proceed(request)
            }

            // Skip JWT for auth, connect and refresh endpoints (refresh-token per spec
            // requires no Authorization; sending an expired JWT here could fail the refresh)
            if (path.startsWith("/api/v3/auth") || path == "/api/v3/connect" || path == "/api/v3/refresh-token") {
                Napier.d("JwtAuthInterceptor - Skipping: auth or connect endpoint", tag = "JwtAuthInterceptor")
                return chain.proceed(request)
            }

            // Get JWT token synchronously
            val jwtToken =
                runBlocking {
                    jwtTokenStorage.jwtToken.first()
                }

            // If no token, proceed without Authorization header
            if (jwtToken == null) {
                Napier.w("JwtAuthInterceptor - No JWT token available", tag = "JwtAuthInterceptor")
                return chain.proceed(request)
            }

            Napier.d("JwtAuthInterceptor - Adding JWT token: Bearer ${jwtToken.accessToken.take(20)}...", tag = "JwtAuthInterceptor")

            // Replace Authorization header with JWT token (overrides bearer set by BearerAuthInterceptor)
            val newRequest =
                request
                    .newBuilder()
                    .header("Authorization", "Bearer ${jwtToken.accessToken}")
                    .build()

            return chain.proceed(newRequest)
        }
    }

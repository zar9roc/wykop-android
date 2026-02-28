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
            if (!path.startsWith("v3/")) {
                return chain.proceed(request)
            }

            // Skip JWT for auth and connect endpoints
            if (path.startsWith("v3/auth") || path == "v3/connect") {
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

            // Replace Authorization header with JWT token (overrides bearer set by BearerAuthInterceptor)
            val newRequest =
                request
                    .newBuilder()
                    .header("Authorization", "Bearer ${jwtToken.accessToken}")
                    .build()

            return chain.proceed(newRequest)
        }
    }

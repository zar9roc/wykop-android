package io.github.wykopmobilny.wykop.remote

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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
        private val jwtTokenStorage: JwtTokenStorage,
    ) : Interceptor {
        companion object {
            // Ile maksymalnie czekamy na starcie na pobranie bearer tokenu (POST /v3/auth
            // leci asynchronicznie w InitializeApp). Po tym czasie przepuszczamy zapytanie
            // bez autoryzacji (i wtedy 403 jest juz realnym bledem, nie wyscigiem).
            private const val BEARER_WAIT_TIMEOUT_MS = 10_000L

            // Brak tokenu przy zerwanej sieci dotyczy kazdego zapytania - bez limitu
            // jedna awaria potrafi wygenerowac kilkadziesiat identycznych ostrzezen.
            private const val NO_TOKEN_LOG_INTERVAL_MS = 30_000L
        }

        @Volatile
        private var lastNoTokenLogAt = 0L

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath
            val url = request.url.toString()

            Napier.d("BearerAuthInterceptor - URL: $url", tag = "BearerAuthInterceptor")
            Napier.d("BearerAuthInterceptor - Path: $path", tag = "BearerAuthInterceptor")

            // Only add bearer token for v3 API endpoints (skip /v3/auth where we obtain it
            // and /v3/refresh-token which per spec requires no Authorization)
            if (!path.startsWith("/api/v3/") || path.startsWith("/api/v3/auth") || path == "/api/v3/refresh-token") {
                Napier.d("BearerAuthInterceptor - Skipping: not v3 API or auth endpoint", tag = "BearerAuthInterceptor")
                return chain.proceed(request)
            }

            // Bearer jest pobierany asynchronicznie tuz po starcie (POST /v3/auth), wiec
            // pierwsze zapytania moga go zastac jako null (wyscig -> 403 "Authentication
            // required"). Czekamy krotko na pierwszy niepusty token. Dla zalogowanego
            // uzytkownika nie czekamy - JwtAuthInterceptor i tak nadpisze naglowek JWT.
            val bearerToken =
                runBlocking {
                    bearerTokenStorage.bearerToken.first()
                        ?: if (jwtTokenStorage.jwtToken.first() != null) {
                            null
                        } else {
                            awaitBearerToken()
                        }
                }

            // If no token, proceed without Authorization header
            if (bearerToken == null) {
                logMissingToken()
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

        /**
         * Czeka na token, ale nie dluzej niz do konca trwajacej proby /v3/auth.
         * Gdy proba zakonczy sie porazka (brak sieci), token juz nie przyjdzie -
         * czekanie pelnych [BEARER_WAIT_TIMEOUT_MS] blokowaloby wtedy kazde
         * zapytanie z osobna.
         */
        private suspend fun awaitBearerToken(): String? {
            val attemptsBefore = bearerTokenStorage.authAttempts.first()
            return withTimeoutOrNull(BEARER_WAIT_TIMEOUT_MS) {
                combine(bearerTokenStorage.bearerToken, bearerTokenStorage.authAttempts, ::Pair)
                    .first { (token, attempts) -> token != null || attempts > attemptsBefore }
                    .first
            }
        }

        /** Ostrzezenie o braku tokenu najwyzej raz na [NO_TOKEN_LOG_INTERVAL_MS]. */
        private fun logMissingToken() {
            val now = System.currentTimeMillis()
            if (now - lastNoTokenLogAt < NO_TOKEN_LOG_INTERVAL_MS) {
                Napier.d("BearerAuthInterceptor - No bearer token available", tag = "BearerAuthInterceptor")
                return
            }
            lastNoTokenLogAt = now
            Napier.w("BearerAuthInterceptor - No bearer token available", tag = "BearerAuthInterceptor")
        }
    }

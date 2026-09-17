package io.github.wykopmobilny.domain.startup

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Pobiera goscinny (aplikacyjny) token JWT przez POST /v3/auth i zapisuje go w
 * [BearerTokenStorage]. Uzywany na starcie aplikacji ([InitializeApp]) oraz po
 * wylogowaniu - swiezy token gwarantuje dzialanie API v3 w trybie goscia
 * (token ze startu procesu moze byc juz przeterminowany).
 *
 * Bledy sieci/HTTP sa logowane i polykane - brak swiezego tokenu nie moze
 * blokowac startu ani wylogowania.
 */
class AuthenticateApp
    @Inject
    internal constructor(
        private val appConfig: AppConfig,
        private val authV3Api: AuthV3RetrofitApi,
        private val bearerTokenStorage: BearerTokenStorage,
    ) {
        suspend operator fun invoke() {
            try {
                val response =
                    authV3Api.authenticate(
                        WykopApiRequestV3(
                            data = AuthRequestV3(key = appConfig.v3ApiKey, secret = appConfig.v3ApiSecret),
                        ),
                    )
                val token = response.data?.token
                if (token != null) {
                    bearerTokenStorage.updateBearerToken(token)
                } else {
                    Napier.w("App auth response missing token: ${response.error?.messagePl}")
                }
            } catch (e: HttpException) {
                Napier.w("App auth failed with HTTP ${e.code()}", e)
            } catch (e: IOException) {
                Napier.w("App auth failed due to network error", e)
            }
        }
    }

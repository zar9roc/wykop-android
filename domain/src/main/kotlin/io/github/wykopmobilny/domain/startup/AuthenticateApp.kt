package io.github.wykopmobilny.domain.startup

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Pobiera goscinny (aplikacyjny) token JWT przez POST /v3/auth i zapisuje go w
 * [BearerTokenStorage]. Uzywany na starcie aplikacji ([InitializeApp]) oraz po
 * wylogowaniu - swiezy token gwarantuje dzialanie API v3 w trybie goscia
 * (token ze startu procesu moze byc juz przeterminowany).
 *
 * Bledy sieci sa ponawiane (na zimnym starcie DNS bywa jeszcze niegotowy), a po
 * wyczerpaniu prob logowane i polykane - brak swiezego tokenu nie moze blokowac
 * startu ani wylogowania. Kazda proba bez tokenu oznacza, ze wszystkie zapytania
 * v3 poleca nieautoryzowane i dostana 403, stad warto sprobowac ponownie.
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
                repeat(MAX_ATTEMPTS) { attempt ->
                    if (tryAuthenticate(attempt)) return@repeat
                    if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAYS[attempt])
                }
            } finally {
                // Takze po porazce - inaczej kazde zapytanie v3 czekaloby pelne
                // 10 s w BearerAuthInterceptor na token, ktory juz nie przyjdzie.
                bearerTokenStorage.onAuthAttemptFinished()
            }
        }

        /** Zwraca true, gdy nie ma sensu ponawiac (sukces albo blad nie-sieciowy). */
        private suspend fun tryAuthenticate(attempt: Int): Boolean =
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
                true
            } catch (e: HttpException) {
                // Zly klucz/sekret nie naprawi sie przez ponowienie.
                Napier.w("App auth failed with HTTP ${e.code()}", e)
                true
            } catch (e: IOException) {
                // Blad sieci jest oczekiwany (samolotowy, brak trasy, DNS jeszcze
                // niegotowy) - sam komunikat wystarczy, stack trace zasmieca log.
                Napier.w("App auth failed due to network error (proba ${attempt + 1}/$MAX_ATTEMPTS): ${e.message}")
                false
            }

        private companion object {
            // Suma odstepow mniejsza niz okno czekania BearerAuthInterceptor (10 s),
            // zeby zapytania startowe zlapaly token z ponowionej proby.
            val RETRY_DELAYS = listOf(1.seconds, 3.seconds)
            val MAX_ATTEMPTS = RETRY_DELAYS.size + 1
        }
    }

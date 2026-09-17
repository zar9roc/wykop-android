package io.github.wykopmobilny.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wykopmobilny.R
import io.github.wykopmobilny.TestApp
import io.github.wykopmobilny.tests.pages.MainPage
import io.github.wykopmobilny.tests.responses.callsOnAppStart
import io.github.wykopmobilny.tests.responses.guestAuth
import io.github.wykopmobilny.utils.waitVisible
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Po wylogowaniu aplikacja ma pobrac swiezy goscinny token JWT (POST /v3/auth) -
 * token ze startu procesu mogl wygasnac, a bez waznego tokenu API v3 nie dziala
 * w trybie goscia.
 */
@RunWith(AndroidJUnit4::class)
class LogoutGuestSessionTest : BaseActivityTest() {
    @Test
    fun logout_generatesFreshGuestToken() {
        launchLoggedInApp()

        // Wylogowanie: pozycja w szufladzie nawigacji + dialog potwierdzenia.
        // Po potwierdzeniu leci POST /v3/auth (mock nizej), a aktywnosc restartuje
        // sie na ekran goscia - stad ponowny komplet mockow startowych.
        mockWebServerRule.guestAuth()
        mockWebServerRule.callsOnAppStart()

        MainPage.openDrawer()
        MainPage.tapDrawerOption(R.id.logout)
        onView(withId(android.R.id.button1)).waitVisible().perform(click())

        val storages = TestApp.instance.storages
        runBlocking {
            withTimeout(timeMillis = 10_000) {
                // Sesja i JWT uzytkownika wyczyszczone...
                storages.userInfoStorage().loggedUser.first { it == null }
                assertNull(storages.jwtTokenStorage().jwtToken.first())
                // ...a goscinny token pochodzi ze SWIEZEJ odpowiedzi /v3/auth,
                // nie ze startu procesu.
                val guestToken = storages.bearerTokenStorage().bearerToken.first { it != null }
                assertEquals("fixture-guest-jwt-token", guestToken)
            }
        }
    }
}

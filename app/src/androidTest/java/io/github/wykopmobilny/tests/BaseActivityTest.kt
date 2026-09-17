package io.github.wykopmobilny.tests

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import io.github.wykopmobilny.TestApp
import io.github.wykopmobilny.storage.api.JwtToken
import io.github.wykopmobilny.storage.api.LoggedUserInfo
import io.github.wykopmobilny.storage.api.UserSession
import io.github.wykopmobilny.tests.responses.callsOnAppStart
import io.github.wykopmobilny.tests.rules.CleanupRule
import io.github.wykopmobilny.tests.rules.DispatcherIdlerRule
import io.github.wykopmobilny.tests.rules.IdlingResourcesRule
import io.github.wykopmobilny.tests.rules.MockWebServerRule
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.RuleChain

abstract class BaseActivityTest {
    val mockWebServerRule = MockWebServerRule()

    @get:Rule
    val rules: RuleChain =
        RuleChain
            .outerRule(IdlingResourcesRule())
            .around(CleanupRule())
            .around(DispatcherIdlerRule())
            .around(mockWebServerRule)

    protected fun logUserIn() =
        runBlocking {
            val storages = TestApp.instance.storages
            storages.sessionStorage().updateSession(UserSession(login = "fixture-user", token = "fixture_token"))
            storages.userInfoStorage().updateLoggedUser(
                LoggedUserInfo(
                    id = "Fixture name",
                    userToken = "fixture_token",
                    avatarUrl = "https://wykop.pl/cdn/avatarfixture-avatar.png",
                    backgroundUrl = null,
                ),
            )
            // Sciezki API v3 sprawdzaja jwtTokenStorage (isJwtAuthorized, JwtAuthInterceptor,
            // odswiezanie powiadomien) - bez tokenu user wygladalby na wylogowanego mimo sesji.
            // expiresAt daleko w przyszlosci = zaden test nie wpadnie w refresh flow.
            storages.jwtTokenStorage().updateJwtToken(
                JwtToken(
                    accessToken = "fixture-jwt-access-token",
                    refreshToken = "fixture-jwt-refresh-token",
                    expiresAt = FIXTURE_JWT_EXPIRES_AT_MS,
                ),
            )
            Espresso.onIdle()
        }

    private companion object {
        // 2100-01-01T00:00:00Z - token nigdy nie wygasa w trakcie testu.
        const val FIXTURE_JWT_EXPIRES_AT_MS = 4_102_444_800_000
    }

    protected fun launchLoggedInApp() {
        logUserIn()
        mockWebServerRule.callsOnAppStart()
        launchActivity<MainNavigationActivity>()
        Espresso.onIdle()
    }
}

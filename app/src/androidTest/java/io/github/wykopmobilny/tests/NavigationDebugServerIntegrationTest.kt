package io.github.wykopmobilny.tests

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wykopmobilny.tests.responses.callsOnAppStart
import io.github.wykopmobilny.tests.utils.DebugHttpClient
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test combining BaseActivityTest infrastructure with DebugHttpServer.
 *
 * This test:
 * 1. Uses MockWebServerRule to mock API responses (like other tests)
 * 2. Launches the app through launchActivity
 * 3. Uses DebugHttpServer to verify navigation without Espresso
 *
 * Benefits:
 * - Self-contained: doesn't require manual app launch
 * - Faster than Espresso tests: no UI interaction overhead
 * - Reliable: uses actual navigation logic, not UI simulation
 *
 * Setup required ONCE before running:
 * ```
 * adb forward tcp:8899 tcp:8899
 * ```
 */
@RunWith(AndroidJUnit4::class)
class NavigationDebugServerIntegrationTest : BaseActivityTest() {

    private lateinit var debugClient: DebugHttpClient

    @Before
    fun setUp() {
        debugClient = DebugHttpClient()

        // Verify debug server is reachable
        // If this fails, run: adb forward tcp:8899 tcp:8899
        assertTrue(
            "DebugHttpServer not reachable. Run: adb forward tcp:8899 tcp:8899",
            debugClient.isServerReachable(),
        )

        // Launch app with mocked API
        mockWebServerRule.callsOnAppStart()
        launchActivity<MainNavigationActivity>()
        Espresso.onIdle()

        // Give server time to start (already started by DebugToolsInitializer)
        Thread.sleep(300)
    }

    @Test
    fun testNavigationViaDebugServer() {
        // Verify initial state
        var screen = debugClient.getScreen()
        assertEquals("MainNavigationActivity", screen.getString("activity"))

        // Navigate to hot
        var navResult = debugClient.navigateTo("hot")
        assertTrue("Navigation to hot failed", navResult.getBoolean("success"))
        Thread.sleep(500)

        screen = debugClient.getScreen()
        val fragment = screen.optString("fragment", "")
        assertTrue(
            "Expected fragment containing 'Hot' but got '$fragment'",
            fragment.contains("Hot", ignoreCase = true),
        )

        // Navigate to promoted
        navResult = debugClient.navigateTo("promoted")
        assertTrue("Navigation to promoted failed", navResult.getBoolean("success"))
        Thread.sleep(500)

        screen = debugClient.getScreen()
        val promotedFragment = screen.optString("fragment", "")
        assertTrue(
            "Expected fragment containing 'Promoted' but got '$promotedFragment'",
            promotedFragment.contains("Promoted", ignoreCase = true),
        )
    }

    @Test
    fun testScreenDataRetrieval() {
        // Navigate to hot
        debugClient.navigateTo("hot")
        Thread.sleep(1000)

        // Verify we can retrieve screen data
        val screen = debugClient.getScreen()
        assertTrue("Should have fragment", screen.has("fragment"))
        assertTrue("Should have adapter info", screen.has("adapter"))

        // Try to get entries (might be empty if API mock doesn't return data)
        val entriesResult = debugClient.getScreenEntries()
        assertTrue("Should have count field", entriesResult.has("count"))
        assertTrue("Should have entries array", entriesResult.has("entries"))
    }

    @Test
    fun testStateQuery() {
        val state = debugClient.getState()

        assertEquals(
            "Should be debug build",
            "io.github.wykopmobilny.debug",
            state.getString("package"),
        )

        assertEquals(
            "Should be in MainNavigationActivity",
            "MainNavigationActivity",
            state.getString("activity"),
        )

        // user_logged_in should be false (no login in this test)
        assertEquals(
            "User should not be logged in",
            false,
            state.optBoolean("user_logged_in", false),
        )
    }

    @Test
    fun testMultipleNavigations() {
        val sequence = listOf("hot", "promoted", "upcoming", "hot")

        for (tab in sequence) {
            val navResult = debugClient.navigateTo(tab)
            assertTrue("Navigation to $tab failed", navResult.getBoolean("success"))
            assertEquals("Tab should match", tab, navResult.getString("tab"))

            Thread.sleep(400)

            val screen = debugClient.getScreen()
            val fragment = screen.optString("fragment", "")
            assertTrue(
                "Fragment should not be empty after navigating to $tab",
                fragment.isNotEmpty(),
            )
        }
    }
}

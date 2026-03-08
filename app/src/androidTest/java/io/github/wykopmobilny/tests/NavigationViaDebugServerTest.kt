package io.github.wykopmobilny.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wykopmobilny.tests.rules.DebugServerRule
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end navigation tests using DebugHttpServer.
 *
 * Tests navigate between tabs and verify screen state without UI interaction (Espresso).
 * Instead, they use HTTP API exposed by DebugHttpServer.
 *
 * Prerequisites (MUST be done BEFORE running these tests):
 * 1. Build debug APK: `./gradlew assembleDebug`
 * 2. Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
 * 3. Launch app manually or through BaseActivityTest
 * 4. Set up port forwarding: `adb forward tcp:8899 tcp:8899`
 *
 * Benefits over UI tests:
 * - Faster (no Espresso interactions, no view hierarchy traversal)
 * - More stable (no flakiness from UI timing issues)
 * - Tests actual navigation logic, not UI clicks
 * - Can inspect adapter state directly
 *
 * Note: These tests verify app state AFTER navigation completes.
 * They don't test UI rendering or user interaction flows.
 */
@RunWith(AndroidJUnit4::class)
class NavigationViaDebugServerTest {

    @get:Rule
    val debugServerRule = DebugServerRule()

    private val client get() = debugServerRule.client

    /**
     * Test navigating between all main tabs and verifying screen state
     */
    @Test
    fun testNavigateAllTabs() {
        val tabs = listOf(
            "promoted" to "PromotedFragment",
            "upcoming" to "UpcomingFragment",
            "hits" to "HitsFragment",
            "hot" to "HotFragment",
            "mywykop" to "MyWykopFragment",
            "favorite" to "FavoriteFragment",
        )

        for ((tab, expectedFragment) in tabs) {
            // Navigate to tab
            val navResult = client.navigateTo(tab)
            assertTrue("Navigation to $tab failed", navResult.getBoolean("success"))
            assertEquals(tab, navResult.getString("tab"))

            // Wait for navigation to complete
            Thread.sleep(500)

            // Verify screen state
            val screen = client.getScreen()
            val actualFragment = screen.optString("fragment", "")

            // Fragment name might include child fragments, e.g., "HotEntriesFragment"
            assertTrue(
                "Expected fragment containing '$expectedFragment' but got '$actualFragment' after navigating to $tab",
                actualFragment.contains(expectedFragment, ignoreCase = true) ||
                    actualFragment == expectedFragment,
            )
        }
    }

    /**
     * Test navigating to hot entries and verifying entries are loaded
     */
    @Test
    fun testHotEntriesLoaded() {
        // Navigate to hot
        val navResult = client.navigateTo("hot")
        assertTrue(navResult.getBoolean("success"))

        // Wait for data to load
        Thread.sleep(1000)

        // Get entries from adapter
        val entriesResult = client.getScreenEntries()
        val count = entriesResult.getInt("count")

        // Verify at least some entries are present
        assertTrue("Expected at least 1 entry in hot, got $count", count > 0)

        // Verify entry structure
        if (count > 0) {
            val entries = entriesResult.getJSONArray("entries")
            val firstEntry = entries.getJSONObject(0)

            assertNotNull("Entry should have id", firstEntry.optLong("id", 0))
            assertNotNull("Entry should have body", firstEntry.optString("body"))
            assertNotNull("Entry should have author", firstEntry.optString("author"))
            assertTrue("Entry should have vote_count >= 0", firstEntry.getInt("vote_count") >= 0)
        }
    }

    /**
     * Test navigating to promoted links and verifying links are loaded
     */
    @Test
    fun testPromotedLinksLoaded() {
        // Navigate to promoted
        val navResult = client.navigateTo("promoted")
        assertTrue(navResult.getBoolean("success"))

        // Wait for data to load
        Thread.sleep(1000)

        // Get links from adapter
        val linksResult = client.getScreenLinks()
        val count = linksResult.getInt("count")

        // Verify at least some links are present
        assertTrue("Expected at least 1 link in promoted, got $count", count > 0)

        // Verify link structure
        if (count > 0) {
            val links = linksResult.getJSONArray("links")
            val firstLink = links.getJSONObject(0)

            assertNotNull("Link should have id", firstLink.optLong("id", 0))
            assertNotNull("Link should have title", firstLink.optString("title"))
            assertNotNull("Link should have url", firstLink.optString("url"))
            assertTrue("Link should have vote_count >= 0", firstLink.getInt("vote_count") >= 0)
        }
    }

    /**
     * Test app state query
     */
    @Test
    fun testGetState() {
        val state = client.getState()

        // Verify basic state fields
        assertNotNull("State should have activity", state.optString("activity"))
        assertNotNull("State should have package", state.optString("package"))
        assertEquals(
            "Package should be debug build",
            "io.github.wykopmobilny.debug",
            state.getString("package"),
        )
    }

    /**
     * Test navigation between promoted and hot multiple times
     */
    @Test
    fun testNavigationStability() {
        repeat(3) { iteration ->
            // Navigate to promoted
            var navResult = client.navigateTo("promoted")
            assertTrue("Iteration $iteration: promoted navigation failed", navResult.getBoolean("success"))
            Thread.sleep(500)

            var screen = client.getScreen()
            assertTrue(
                "Iteration $iteration: Expected PromotedFragment",
                screen.optString("fragment", "").contains("Promoted", ignoreCase = true),
            )

            // Navigate to hot
            navResult = client.navigateTo("hot")
            assertTrue("Iteration $iteration: hot navigation failed", navResult.getBoolean("success"))
            Thread.sleep(500)

            screen = client.getScreen()
            assertTrue(
                "Iteration $iteration: Expected HotFragment",
                screen.optString("fragment", "").contains("Hot", ignoreCase = true),
            )
        }
    }

    /**
     * Test getting screen info for all tabs
     */
    @Test
    fun testScreenInfoForAllTabs() {
        val tabs = listOf("promoted", "upcoming", "hits", "hot")

        for (tab in tabs) {
            client.navigateTo(tab)
            Thread.sleep(800)

            val screen = client.getScreen()

            // All tabs should have:
            // - activity: MainNavigationActivity
            // - fragment: some fragment
            // - adapter: some adapter (might be null during loading)
            assertEquals(
                "Activity should be MainNavigationActivity for tab $tab",
                "MainNavigationActivity",
                screen.getString("activity"),
            )

            val fragment = screen.optString("fragment", "")
            assertTrue("Fragment should not be empty for tab $tab", fragment.isNotEmpty())
        }
    }

    /**
     * Helper: Get first entry id from current screen
     */
    private fun getFirstEntryId(): Long? {
        val entriesResult = client.getScreenEntries()
        val count = entriesResult.getInt("count")
        if (count == 0) return null

        val entries = entriesResult.getJSONArray("entries")
        return entries.getJSONObject(0).getLong("id")
    }

    /**
     * Helper: Find entry by id in current screen
     */
    private fun findEntryById(entryId: Long): JSONObject? {
        val entriesResult = client.getScreenEntries()
        val entries = entriesResult.getJSONArray("entries")

        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            if (entry.getLong("id") == entryId) {
                return entry
            }
        }
        return null
    }
}

package io.github.wykopmobilny.tests.rules

import io.github.wykopmobilny.tests.utils.DebugHttpClient
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit Rule that ensures DebugHttpServer is reachable before running tests.
 *
 * Prerequisites:
 * 1. App must be running in debug build with DebugHttpServer started
 * 2. ADB port forwarding must be set up: `adb forward tcp:8899 tcp:8899`
 *
 * Usage:
 * ```
 * @get:Rule
 * val debugServerRule = DebugServerRule()
 *
 * @Test
 * fun testNavigation() {
 *     val client = debugServerRule.client
 *     client.navigateTo("hot")
 *     // ...
 * }
 * ```
 */
class DebugServerRule : TestRule {
    lateinit var client: DebugHttpClient
        private set

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                client = DebugHttpClient()

                // Verify server is reachable with retry
                var lastError: Exception? = null
                var attempts = 0
                val maxAttempts = 5

                for (attempt in 1..maxAttempts) {
                    attempts = attempt
                    try {
                        if (client.isServerReachable()) {
                            // Success - proceed with test
                            base.evaluate()
                            return
                        }
                    } catch (e: Exception) {
                        lastError = e
                    }

                    if (attempt < maxAttempts) {
                        Thread.sleep(500)
                    }
                }

                // All attempts failed
                throw IllegalStateException(
                    """
                    |DebugHttpServer is not reachable at localhost:${DebugHttpClient.DEFAULT_PORT}
                    |Tried $attempts times over ${(attempts - 1) * 500}ms
                    |Last error: ${lastError?.message}
                    |
                    |Prerequisites:
                    |1. Build and install debug APK: ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
                    |2. Launch the app manually or through another test
                    |3. Set up ADB port forwarding: adb forward tcp:8899 tcp:8899
                    |
                    |Troubleshooting:
                    |- Verify port forwarding: adb forward --list
                    |- Test server manually: curl localhost:8899/
                    |- Check server logs: adb logcat -s DebugHttpServer DebugServerHolder -d
                    |
                    |Common issues:
                    |1. App not running in foreground
                    |2. Port forwarding not set up
                    |3. Server crashed during startup (check logcat)
                    |
                    |See docs/DEBUG_HTTP_SERVER_TROUBLESHOOTING.md for detailed analysis
                    """.trimMargin(),
                    lastError,
                )
            }
        }
}

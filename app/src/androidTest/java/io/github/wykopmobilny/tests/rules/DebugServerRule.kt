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

                // Verify server is reachable
                if (!client.isServerReachable()) {
                    throw IllegalStateException(
                        """
                        |DebugHttpServer is not reachable at localhost:${DebugHttpClient.DEFAULT_PORT}
                        |
                        |Prerequisites:
                        |1. Build and install debug APK: ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
                        |2. Launch the app manually or through another test
                        |3. Set up ADB port forwarding: adb forward tcp:8899 tcp:8899
                        |
                        |Verify server is running: curl localhost:8899/
                        """.trimMargin(),
                    )
                }

                try {
                    base.evaluate()
                } finally {
                    // Cleanup if needed
                }
            }
        }
}

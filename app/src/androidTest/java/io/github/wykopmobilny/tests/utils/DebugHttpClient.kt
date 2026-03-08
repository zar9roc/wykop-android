package io.github.wykopmobilny.tests.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for communicating with DebugHttpServer during tests.
 * Provides typed methods for all debug server endpoints.
 *
 * Usage:
 * ```
 * val client = DebugHttpClient()
 * client.navigateTo("hot")
 * val state = client.getState()
 * ```
 */
class DebugHttpClient(
    private val baseUrl: String = "http://localhost:$DEFAULT_PORT",
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * GET /state - Get current app state
     */
    fun getState(verbose: Boolean = false): JSONObject {
        val url = if (verbose) "$baseUrl/state?verbose=true" else "$baseUrl/state"
        return get(url)
    }

    /**
     * GET /screen - Get current screen summary (activity, fragment, adapter, item count)
     */
    fun getScreen(): JSONObject = get("$baseUrl/screen")

    /**
     * GET /screen/entries - Get entries from current adapter
     */
    fun getScreenEntries(): JSONObject = get("$baseUrl/screen/entries")

    /**
     * GET /screen/links - Get links from current adapter
     */
    fun getScreenLinks(): JSONObject = get("$baseUrl/screen/links")

    /**
     * POST /navigate/{tab} - Navigate to specified tab
     * @param tab One of: promoted, upcoming, hits, hot, mywykop, favorite, search, messages, notifications
     */
    fun navigateTo(tab: String): JSONObject = post("$baseUrl/navigate/$tab")

    /**
     * POST /action/vote/entry/{id} - Vote on entry
     */
    fun voteEntry(entryId: Long): JSONObject = post("$baseUrl/action/vote/entry/$entryId")

    /**
     * DELETE /action/vote/entry/{id} - Unvote entry
     */
    fun unvoteEntry(entryId: Long): JSONObject = delete("$baseUrl/action/vote/entry/$entryId")

    /**
     * POST /action/clear-cache - Clear app cache
     */
    fun clearCache(): JSONObject = post("$baseUrl/action/clear-cache")

    /**
     * POST /action/logout - Force logout
     */
    fun logout(): JSONObject = post("$baseUrl/action/logout")

    /**
     * Check if debug server is reachable
     */
    fun isServerReachable(): Boolean = try {
        get("$baseUrl/")
        true
    } catch (e: Exception) {
        false
    }

    // --- HTTP primitives ---

    private fun get(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw IllegalStateException("Empty response from $url")

            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} from $url: $body")
            }

            return JSONObject(body)
        }
    }

    private fun post(url: String, body: String = ""): JSONObject {
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody())
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
                ?: throw IllegalStateException("Empty response from $url")

            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} from $url: $responseBody")
            }

            return JSONObject(responseBody)
        }
    }

    private fun delete(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string()
                ?: throw IllegalStateException("Empty response from $url")

            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} from $url: $responseBody")
            }

            return JSONObject(responseBody)
        }
    }

    companion object {
        const val DEFAULT_PORT = 8899
    }
}

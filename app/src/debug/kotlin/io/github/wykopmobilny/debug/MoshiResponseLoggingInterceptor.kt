package io.github.wykopmobilny.debug

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import io.github.aakira.napier.Napier
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

/**
 * OkHttp interceptor that logs parsed Moshi responses for debugging.
 *
 * This interceptor:
 * 1. Intercepts successful HTTP responses (2xx)
 * 2. Attempts to parse the JSON body as a generic Map
 * 3. Logs the parsed data structure and pagination to Logcat via Napier
 * 4. Preserves the original response body for Retrofit
 *
 * Uses Map parsing instead of WykopApiResponseV3<Any> to avoid PhpArrayAdapterFactory
 * issues with generic Any type.
 *
 * Only active in debug builds. Use with Logcat filter: tag:MoshiResponse
 *
 * @param moshi Moshi instance with all adapters configured
 */
class MoshiResponseLoggingInterceptor(
    private val moshi: Moshi,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Only process successful API v3 responses
        if (!response.isSuccessful || !request.url.encodedPath.startsWith("/api/v3/")) {
            return response
        }

        // Skip 204 No Content responses (empty body, common for DELETE endpoints)
        if (response.code == 204) {
            return response
        }

        val responseBody = response.body ?: return response

        try {
            // Read response body (we need to preserve it for Retrofit)
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // Buffer entire body
            val buffer = source.buffer.clone()
            val bodyString = buffer.readUtf8()

            // Parse as Map to avoid PhpArrayAdapterFactory issues with Any type
            val adapter = moshi.adapter<Map<String, Any>>(Map::class.java)
            val jsonMap = adapter.fromJson(bodyString)

            if (jsonMap != null) {
                logParsedResponse(request.url.encodedPath, jsonMap, bodyString)
            }

            // Return response with preserved body
            return response
                .newBuilder()
                .body(bodyString.toResponseBody(responseBody.contentType()))
                .build()
        } catch (e: IOException) {
            // Network error during parsing
            Napier.w(
                tag = "MoshiResponse",
                message = "IO error while parsing response for ${request.url.encodedPath}: ${e.message}",
                throwable = e,
            )
            return response
        } catch (e: JsonDataException) {
            // Malformed JSON data
            Napier.w(
                tag = "MoshiResponse",
                message = "JSON data error for ${request.url.encodedPath}: ${e.message}",
                throwable = e,
            )
            return response
        } catch (e: JsonEncodingException) {
            // JSON encoding error
            Napier.w(
                tag = "MoshiResponse",
                message = "JSON encoding error for ${request.url.encodedPath}: ${e.message}",
                throwable = e,
            )
            return response
        }
    }

    private fun logParsedResponse(
        path: String,
        jsonMap: Map<String, Any>,
        rawJson: String,
    ) {
        val data = jsonMap["data"]
        val dataType = data?.javaClass?.simpleName ?: "null"
        val dataPreview =
            when (data) {
                is List<*> -> "List<${data.firstOrNull()?.javaClass?.simpleName}>(${data.size} items)"
                is Map<*, *> -> "Map(${data.size} entries)"
                else -> data?.toString()?.take(100) ?: "null"
            }

        @Suppress("UNCHECKED_CAST")
        val pagination = jsonMap["pagination"] as? Map<String, Any>

        val logMessage =
            buildString {
                appendLine("╔═══════════════════════════════════════════════════════════════")
                appendLine("║ Moshi Parsed Response")
                appendLine("╠═══════════════════════════════════════════════════════════════")
                appendLine("║ Endpoint: $path")
                appendLine("║ Data Type: $dataType")
                appendLine("║ Data Preview: $dataPreview")
                if (pagination != null) {
                    appendLine("║ Pagination:")
                    appendLine("║   - per_page: ${pagination["per_page"]}")
                    appendLine("║   - total: ${pagination["total"]}")
                    appendLine("║   - next: ${pagination["next"] ?: "null"}")
                } else {
                    appendLine("║ Pagination: null")
                }
                appendLine("╠═══════════════════════════════════════════════════════════════")
                appendLine("║ Parsed Map Keys:")
                appendLine("║ ${jsonMap.keys}")
                appendLine("╠═══════════════════════════════════════════════════════════════")
                appendLine("║ Raw JSON (first 500 chars):")
                appendLine("║ ${rawJson.take(500)}")
                appendLine("╚═══════════════════════════════════════════════════════════════")
            }

        Napier.d(logMessage, tag = "MoshiResponse")
    }
}

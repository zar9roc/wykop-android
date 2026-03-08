package io.github.wykopmobilny.debug

import com.squareup.moshi.Moshi
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * OkHttp interceptor that logs parsed Moshi responses for debugging.
 *
 * This interceptor:
 * 1. Intercepts successful HTTP responses (2xx)
 * 2. Attempts to parse the JSON body as WykopApiResponseV3<*>
 * 3. Logs the parsed object structure to Logcat via Napier
 * 4. Preserves the original response body for Retrofit
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

        val responseBody = response.body ?: return response

        try {
            // Read response body (we need to preserve it for Retrofit)
            val source = responseBody.source()
            source.request(Long.MAX_VALUE) // Buffer entire body
            val buffer = source.buffer.clone()
            val bodyString = buffer.readUtf8()

            // Parse as generic WykopApiResponseV3
            val type = createParameterizedType(WykopApiResponseV3::class.java, Any::class.java)
            val adapter = moshi.adapter<WykopApiResponseV3<Any>>(type)

            val parsedResponse = adapter.fromJson(bodyString)

            if (parsedResponse != null) {
                logParsedResponse(request.url.encodedPath, parsedResponse, bodyString)
            }

            // Return response with preserved body
            return response.newBuilder()
                .body(bodyString.toResponseBody(responseBody.contentType()))
                .build()
        } catch (e: Exception) {
            // If parsing fails, log error but don't break the chain
            Napier.w(
                tag = "MoshiResponse",
                message = "Failed to parse response for ${request.url.encodedPath}: ${e.message}",
                throwable = e,
            )
            return response
        }
    }

    private fun logParsedResponse(
        path: String,
        response: WykopApiResponseV3<Any>,
        rawJson: String,
    ) {
        val dataType = response.data?.javaClass?.simpleName ?: "null"
        val dataPreview = when (val data = response.data) {
            is List<*> -> "List<${data.firstOrNull()?.javaClass?.simpleName}>(${data.size} items)"
            is Map<*, *> -> "Map(${data.size} entries)"
            else -> data?.toString()?.take(100) ?: "null"
        }

        val pagination = response.pagination

        val logMessage = buildString {
            appendLine("╔═══════════════════════════════════════════════════════════════")
            appendLine("║ Moshi Parsed Response")
            appendLine("╠═══════════════════════════════════════════════════════════════")
            appendLine("║ Endpoint: $path")
            appendLine("║ Data Type: $dataType")
            appendLine("║ Data Preview: $dataPreview")
            if (pagination != null) {
                appendLine("║ Pagination:")
                appendLine("║   - per_page: ${pagination.perPage}")
                appendLine("║   - total: ${pagination.total}")
                appendLine("║   - next: ${pagination.next ?: "null"}")
            } else {
                appendLine("║ Pagination: null")
            }
            appendLine("╠═══════════════════════════════════════════════════════════════")
            appendLine("║ Parsed Object:")
            appendLine("║ $response")
            appendLine("╠═══════════════════════════════════════════════════════════════")
            appendLine("║ Raw JSON (first 500 chars):")
            appendLine("║ ${rawJson.take(500)}")
            appendLine("╚═══════════════════════════════════════════════════════════════")
        }

        Napier.d(logMessage, tag = "MoshiResponse")
    }

    private fun createParameterizedType(rawType: Type, vararg typeArguments: Type): ParameterizedType {
        return object : ParameterizedType {
            override fun getRawType() = rawType
            override fun getActualTypeArguments() = typeArguments
            override fun getOwnerType() = null
        }
    }
}

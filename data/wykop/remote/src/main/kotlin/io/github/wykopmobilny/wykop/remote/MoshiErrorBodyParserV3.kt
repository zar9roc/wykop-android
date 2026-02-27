package io.github.wykopmobilny.wykop.remote

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import dagger.Reusable
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.responses.v3.common.WykopErrorResponseV3
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.IOException
import javax.inject.Inject

/**
 * Moshi-based implementation of [ErrorBodyParserV3].
 *
 * Parses API v3 error responses from HTTP error bodies (4xx, 5xx status codes).
 */
@Reusable
internal class MoshiErrorBodyParserV3
    @Inject
    constructor(
        private val moshi: Moshi,
    ) : ErrorBodyParserV3 {
        private val adapter by lazy {
            moshi.adapter(WykopErrorResponseV3::class.java)
        }

        override suspend fun parse(body: ResponseBody) =
            withContext(AppDispatchers.Default) {
                try {
                    adapter.fromJson(body.source())
                } catch (e: JsonDataException) {
                    // JSON structure doesn't match expected format
                    Napier.w(message = "Failed to parse API v3 error response: ${e.message}", throwable = e)
                    null
                } catch (e: JsonEncodingException) {
                    // JSON encoding issue
                    Napier.w(message = "Failed to parse API v3 error response: ${e.message}", throwable = e)
                    null
                } catch (e: IOException) {
                    // I/O error while reading response body
                    Napier.w(message = "Failed to read API v3 error response: ${e.message}", throwable = e)
                    null
                }
            }
    }

package io.github.wykopmobilny.wykop.remote

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.Reusable
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.ErrorBodyParser
import io.github.wykopmobilny.api.responses.WykopApiResponse
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.IOException
import javax.inject.Inject

@Reusable
internal class MoshiErrorBodyParser
    @Inject
    constructor(
        private val moshi: Moshi,
    ) : ErrorBodyParser {
        private val adapter by lazy {
            moshi.adapter<WykopApiResponse<Any>>(Types.newParameterizedType(WykopApiResponse::class.java, Any::class.java))
        }

        override suspend fun parse(body: ResponseBody) =
            withContext(AppDispatchers.Default) {
                // Check Content-Type before attempting to parse as JSON
                val contentType = body.contentType()
                if (contentType != null && contentType.toString().contains("text/html", ignoreCase = true)) {
                    Napier.w(
                        message =
                            "API v2 returned HTML instead of JSON. " +
                                "This usually indicates a server error (500) or maintenance page. " +
                                "Content-Type: $contentType",
                    )
                    return@withContext null
                }

                try {
                    val source = adapter.fromJson(body.source())
                    source?.error
                } catch (e: JsonDataException) {
                    // JSON structure doesn't match expected format
                    Napier.w(message = "Failed to parse API v2 error response: ${e.message}", throwable = e)
                    null
                } catch (e: JsonEncodingException) {
                    // JSON encoding issue
                    Napier.w(message = "Failed to parse API v2 error response: ${e.message}", throwable = e)
                    null
                } catch (e: IOException) {
                    // I/O error while reading response body
                    Napier.w(message = "Failed to read API v2 error response: ${e.message}", throwable = e)
                    null
                }
            }
    }

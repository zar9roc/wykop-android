package io.github.wykopmobilny.api.errorhandler

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer

/**
 * Error handler transformer for Wykop API v3 responses.
 *
 * Unwraps [WykopApiResponseV3] and extracts the data field.
 * Note: API v3 does not return errors in the response body when successful.
 * HTTP errors (4xx, 5xx) are handled by Retrofit's HttpException and UserTokenRefresher.
 */
class ErrorHandlerTransformerV3<T : Any> : SingleTransformer<WykopApiResponseV3<T>, T> {
    override fun apply(upstream: Single<WykopApiResponseV3<T>>): SingleSource<T> =
        upstream.flatMap { response ->
            val data = response.data
            if (data != null) {
                Single.just(data)
            } else {
                // This should not happen for successful responses (HTTP 2xx)
                // but handle it gracefully in case API returns empty data
                Single.error(IllegalStateException("API v3 response contains null data"))
            }
        }
}

/**
 * Unwraps [WykopApiResponseV3] for suspend functions.
 *
 * @param block suspend function that returns [WykopApiResponseV3]
 * @return unwrapped data from the response
 * @throws IllegalStateException if data is null
 */
suspend fun <T> unwrappingV3(block: suspend () -> WykopApiResponseV3<T>): T {
    val response = block()
    val data = response.data

    return data ?: error("API v3 response contains null data")
}

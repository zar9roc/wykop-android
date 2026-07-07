package io.github.wykopmobilny.api.errorhandler

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.responses.v3.common.ErrorDetailsV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.SingleTransformer
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException

/**
 * Error handler transformer for Wykop API v3 responses.
 *
 * Unwraps [WykopApiResponseV3] and extracts the data field.
 * Parses HTTP error responses (4xx, 5xx) and throws [WykopExceptionParser.WykopApiException]
 * with the error message from API v3.
 */
class ErrorHandlerTransformerV3<T : Any>(
    private val errorBodyParser: ErrorBodyParserV3,
) : SingleTransformer<WykopApiResponseV3<T>, T> {
    override fun apply(upstream: Single<WykopApiResponseV3<T>>): SingleSource<T> =
        upstream
            .flatMap { response ->
                val data = response.data
                if (data != null) {
                    Single.just(data)
                } else {
                    Single.error(IllegalStateException("API v3 response contains null data"))
                }
            }.onErrorResumeNext { throwable ->
                if (throwable is HttpException) {
                    Single.error(parseHttpException(throwable))
                } else {
                    Single.error(throwable)
                }
            }

    private fun parseHttpException(e: HttpException): Throwable {
        val errorBody = e.response()?.errorBody() ?: return e
        return try {
            val errorResponse = runBlocking { errorBodyParser.parse(errorBody) }
            val errorDetails = errorResponse?.error
            if (errorDetails != null) {
                WykopExceptionParser.WykopApiException(
                    code = errorResponse.code,
                    message = errorDetails.toReadableMessage(),
                )
            } else {
                e
            }
        } catch (parseError: IOException) {
            Napier.w(
                message = "Failed to parse API v3 error body: ${parseError.message}",
                throwable = parseError,
            )
            e
        } catch (parseError: JsonDataException) {
            Napier.w(
                message = "Failed to parse API v3 error body: ${parseError.message}",
                throwable = parseError,
            )
            e
        } catch (parseError: JsonEncodingException) {
            Napier.w(
                message = "Failed to parse API v3 error body: ${parseError.message}",
                throwable = parseError,
            )
            e
        }
    }
}

/**
 * Unwraps [WykopApiResponseV3] for suspend functions.
 * Parses HTTP error responses and throws [WykopExceptionParser.WykopApiException].
 *
 * @param errorBodyParser parser for API v3 error bodies
 * @param block suspend function that returns [WykopApiResponseV3]
 * @return unwrapped data from the response
 * @throws WykopExceptionParser.WykopApiException if API returns an error
 * @throws IllegalStateException if data is null
 */
/**
 * Zamienia szczegoly walidacji z serwera (message "Validate" + mapa data)
 * na czytelny komunikat. Bez detali zwraca oryginalny message.
 */
internal fun ErrorDetailsV3.toReadableMessage(): String {
    val codes = data?.values?.flatten().orEmpty()
    return when {
        // Serwer wymaga min. 5 znakow tresci (chyba ze dodano zdjecie/embed).
        codes.any { it == "too_short" } ->
            "Treść jest za krótka – wpis bez zdjęcia musi mieć minimum 5 znaków."
        codes.any { it == "not_blank_content" || it == "not_blank" } ->
            "Treść nie może być pusta."
        codes.any { it == "too_long" } ->
            "Treść jest za długa."
        else -> message
    }
}

suspend fun <T> unwrappingV3(
    errorBodyParser: ErrorBodyParserV3,
    block: suspend () -> WykopApiResponseV3<T>,
): T {
    try {
        val response = block()
        return response.data ?: error("API v3 response contains null data")
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()
        if (errorBody != null) {
            try {
                val errorResponse = errorBodyParser.parse(errorBody)
                val errorDetails = errorResponse?.error
                if (errorDetails != null) {
                    throw WykopExceptionParser.WykopApiException(
                        code = errorResponse.code,
                        message = errorDetails.message,
                    )
                }
            } catch (rethrow: WykopExceptionParser.WykopApiException) {
                throw rethrow
            } catch (parseError: IOException) {
                Napier.w(
                    message = "Failed to parse API v3 error body: ${parseError.message}",
                    throwable = parseError,
                )
            } catch (parseError: JsonDataException) {
                Napier.w(
                    message = "Failed to parse API v3 error body: ${parseError.message}",
                    throwable = parseError,
                )
            } catch (parseError: JsonEncodingException) {
                Napier.w(
                    message = "Failed to parse API v3 error body: ${parseError.message}",
                    throwable = parseError,
                )
            }
        }
        throw e
    }
}

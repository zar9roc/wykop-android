package io.github.wykopmobilny.api.exceptions

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3
import retrofit2.HttpException
import java.io.IOException

/**
 * Sealed interface representing errors that can occur during media upload.
 */
sealed interface MediaUploadException {
    val message: String
    val httpCode: Int?

    /**
     * File size exceeds server limits (HTTP 413 Payload Too Large).
     */
    data class FileTooLarge(
        override val message: String = "Plik jest za duży. Maksymalny rozmiar pliku wynosi 10MB.",
        override val httpCode: Int = 413,
    ) : Exception(message),
        MediaUploadException

    /**
     * File type is not supported by the server (HTTP 415 Unsupported Media Type).
     */
    data class UnsupportedMediaType(
        override val message: String = "Nieobsługiwany typ pliku. Dozwolone formaty: JPEG, PNG, GIF.",
        override val httpCode: Int = 415,
    ) : Exception(message),
        MediaUploadException

    /**
     * Unknown upload error occurred.
     */
    data class Unknown(
        override val message: String,
        override val httpCode: Int?,
        override val cause: Throwable? = null,
    ) : Exception(message, cause),
        MediaUploadException
}

/**
 * Handles media upload with proper error mapping.
 *
 * Maps HTTP errors to specific [MediaUploadException] types:
 * - 413 Payload Too Large → [MediaUploadException.FileTooLarge]
 * - 415 Unsupported Media Type → [MediaUploadException.UnsupportedMediaType]
 * - Other errors → [MediaUploadException.Unknown]
 *
 * @param block suspend function that performs the upload
 * @return [PhotoResponseV3] on success
 * @throws MediaUploadException on upload failure
 */
suspend fun handleMediaUpload(block: suspend () -> WykopApiResponseV3<PhotoResponseV3>): PhotoResponseV3 {
    try {
        val response = block()
        return response.data ?: throw MediaUploadException.Unknown(
            message = "Upload zakończył się sukcesem, ale brak klucza zdjęcia w odpowiedzi",
            httpCode = null,
        )
    } catch (e: HttpException) {
        throw when (e.code()) {
            413 -> {
                MediaUploadException.FileTooLarge()
            }

            415 -> {
                MediaUploadException.UnsupportedMediaType()
            }

            else -> {
                MediaUploadException.Unknown(
                    message = "Błąd podczas uploadu pliku: ${e.message()}",
                    httpCode = e.code(),
                    cause = e,
                )
            }
        }
    } catch (e: IOException) {
        throw MediaUploadException.Unknown(
            message = "Błąd sieciowy podczas uploadu pliku",
            httpCode = null,
            cause = e,
        )
    }
}

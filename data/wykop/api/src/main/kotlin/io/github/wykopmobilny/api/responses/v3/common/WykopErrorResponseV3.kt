package io.github.wykopmobilny.api.responses.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WykopErrorResponseV3(
    @field:Json(name = "code") val code: Int,
    @field:Json(name = "hash") val hash: String?,
    @field:Json(name = "error") val error: ErrorDetailsV3?,
)

@JsonClass(generateAdapter = true)
data class ErrorDetailsV3(
    @field:Json(name = "message") val message: String,
    // Szczegoly walidacji: mapa pole/formularz -> lista kodow bledow
    // (np. {"content":["too_short"]}, {"entry_form":["not_blank_content"]}).
    @field:Json(name = "data") val data: Map<String, List<String>>? = null,
    // UWAGA: pole "key" serwera bywa liczba ("key":0) - zadeklarowane jako String?
    // wywalalo parsing calego bledu (JsonDataException), przez co uzytkownik widzial
    // surowa fraze HTTP zamiast tresci. Pominiete (Moshi ignoruje nieznane pola).
)

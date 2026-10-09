package io.github.wykopmobilny.api.requests.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wpis przez /v3/entries-threads (dodanie i edycja). Tylko ten endpoint przyjmuje
 * galerie "photos" (max 4) - stare /v3/entries odrzuca to pole bledem 409
 * "This form should not contain extra fields". Edycja zastepuje wszystkie media:
 * niepodane zdjecia/embed/ankieta sa usuwane.
 */
@JsonClass(generateAdapter = true)
data class CreateThreadEntryRequestV3(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "photos") val photos: List<String>? = null,
    @field:Json(name = "embed") val embed: String? = null,
    @field:Json(name = "survey") val survey: String? = null,
    @field:Json(name = "adult") val adult: Boolean? = null,
)

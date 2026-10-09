package io.github.wykopmobilny.api.requests.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Komentarz w strukturze watku (/v3/entries-threads/...). W odroznieniu od
 * zwyklego komentarza zdjecia ida jako kolekcja "photos" (galeria, max 4) -
 * pojedyncze pole "photo" ten endpoint po cichu ignoruje.
 */
@JsonClass(generateAdapter = true)
data class CreateThreadCommentRequestV3(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "photos") val photos: List<String>? = null,
    @field:Json(name = "embed") val embed: String? = null,
    @field:Json(name = "adult") val adult: Boolean? = null,
)

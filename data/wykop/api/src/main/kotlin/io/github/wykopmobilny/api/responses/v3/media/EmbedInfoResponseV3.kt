package io.github.wykopmobilny.api.responses.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Odpowiedz POST /media/embed (zweryfikowana na zywo):
// {"data":{"key":"...","type":"youtube","thumbnail":"...","url":"...",
//   "age_category":"all","video_metadata":null,"commercial":false}}
@JsonClass(generateAdapter = true)
data class EmbedInfoResponseV3(
    @field:Json(name = "key") val key: String?,
    @field:Json(name = "type") val type: String?,
    @field:Json(name = "thumbnail") val thumbnail: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "age_category") val ageCategory: String?,
    @field:Json(name = "commercial") val commercial: Boolean?,
)

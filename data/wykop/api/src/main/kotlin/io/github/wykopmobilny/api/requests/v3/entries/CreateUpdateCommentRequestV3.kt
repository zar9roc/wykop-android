package io.github.wykopmobilny.api.requests.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateUpdateCommentRequestV3(
    @field:Json(name = "content") val content: String,
    @field:Json(name = "photo") val photo: String? = null,
    // Galeria mikrobloga (2-4 zdjecia); pojedyncze zdjecie nadal idzie w "photo".
    @field:Json(name = "photos") val photos: List<String>? = null,
    @field:Json(name = "embed") val embed: String? = null,
    @field:Json(name = "adult") val adult: Boolean? = null,
)

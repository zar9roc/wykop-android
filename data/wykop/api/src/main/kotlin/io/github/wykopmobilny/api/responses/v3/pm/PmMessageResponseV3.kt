package io.github.wykopmobilny.api.responses.v3.pm

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import kotlinx.datetime.Instant

@JsonClass(generateAdapter = true)
data class PmMessageResponseV3(
    @field:Json(name = "key") val key: String?,
    @field:Json(name = "created_at") val createdAt: Instant?,
    @field:Json(name = "content") val content: String?,
    // 0 = zalogowany uzytkownik, 1 = rozmowca
    @field:Json(name = "type") val type: Int?,
    @field:Json(name = "adult") val adult: Boolean?,
    @field:Json(name = "media") val media: MediaResponseV3?,
    @field:Json(name = "read") val read: Boolean?,
)

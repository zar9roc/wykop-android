package io.github.wykopmobilny.api.responses.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaResponseV3(
    @field:Json(name = "photo") val photo: PhotoResponseV3?,
    @field:Json(name = "embed") val embed: EmbedResponseV3?,
)

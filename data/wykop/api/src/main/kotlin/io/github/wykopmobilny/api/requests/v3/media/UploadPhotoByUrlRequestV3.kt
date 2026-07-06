package io.github.wykopmobilny.api.requests.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UploadPhotoByUrlRequestV3(
    @field:Json(name = "url") val url: String,
)

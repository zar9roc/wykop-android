package io.github.wykopmobilny.api.requests.v3.notes

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NoteRequestV3(
    @field:Json(name = "content") val content: String,
)

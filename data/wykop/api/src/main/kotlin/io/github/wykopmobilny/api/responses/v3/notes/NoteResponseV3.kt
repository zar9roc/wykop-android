package io.github.wykopmobilny.api.responses.v3.notes

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3

@JsonClass(generateAdapter = true)
data class NoteResponseV3(
    @field:Json(name = "user") val user: UserShortResponseV3,
    @field:Json(name = "content") val content: String?,
)

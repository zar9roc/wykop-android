package io.github.wykopmobilny.api.responses.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CommentsResponseV3(
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "items") val items: List<EntryCommentResponseV3>?,
)

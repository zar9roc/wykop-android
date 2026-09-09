package io.github.wykopmobilny.api.requests.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// POST /media/embed - rejestracja linku do wspieranego serwisu (YouTube, streamable...).
// Zwrocony klucz wysyla sie potem w polu "embed" wpisu/komentarza/wiadomosci.
@JsonClass(generateAdapter = true)
data class CreateEmbedRequestV3(
    @field:Json(name = "url") val url: String,
)

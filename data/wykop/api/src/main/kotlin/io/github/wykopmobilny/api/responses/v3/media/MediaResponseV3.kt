package io.github.wykopmobilny.api.responses.v3.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.entries.SurveyResponseV3

@JsonClass(generateAdapter = true)
data class MediaResponseV3(
    @field:Json(name = "photo") val photo: PhotoResponseV3?,
    @field:Json(name = "embed") val embed: EmbedResponseV3?,
    // Ankieta przychodzi ZAGNIEZDZONA w media (media.survey), nie na top-level wpisu.
    @field:Json(name = "survey") val survey: SurveyResponseV3?,
)

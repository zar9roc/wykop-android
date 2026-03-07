package io.github.wykopmobilny.api.requests.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VoteSurveyRequestV3(
    @field:Json(name = "vote") val vote: Int,
)

package io.github.wykopmobilny.api.responses.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateSurveyResponseV3(
    @field:Json(name = "survey_id") val surveyId: String,
)

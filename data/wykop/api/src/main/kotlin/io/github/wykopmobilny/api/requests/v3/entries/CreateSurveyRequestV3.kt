package io.github.wykopmobilny.api.requests.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// POST /v3/entries/survey - tworzy ankiete i zwraca survey_id, ktore dokleja sie do
// wpisu przez pole CreateUpdateEntryRequestV3.survey.
@JsonClass(generateAdapter = true)
data class CreateSurveyRequestV3(
    @field:Json(name = "question") val question: String,
    @field:Json(name = "answers") val answers: List<String>,
)

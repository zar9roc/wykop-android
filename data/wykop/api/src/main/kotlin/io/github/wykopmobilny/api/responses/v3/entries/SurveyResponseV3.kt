package io.github.wykopmobilny.api.responses.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SurveyResponseV3(
    @field:Json(name = "question") val question: String,
    @field:Json(name = "answers") val answers: List<SurveyAnswerResponseV3>,
    @field:Json(name = "votes_count") val votesCount: Int,
    @field:Json(name = "user_answer") val userAnswer: Int?,
)

@JsonClass(generateAdapter = true)
data class SurveyAnswerResponseV3(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "answer") val answer: String,
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "percentage") val percentage: Double,
)

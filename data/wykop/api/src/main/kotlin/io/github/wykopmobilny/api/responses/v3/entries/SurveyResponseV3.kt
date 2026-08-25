package io.github.wykopmobilny.api.responses.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Realna odpowiedz API v3 (zweryfikowana na zywo): ankieta ma `count` (suma glosow) i
// top-level `voted` (id odpowiedzi zaznaczonej przez usera, 0 = brak); odpowiedz ma `text`
// (nie `answer`) i `voted` (0/1), a NIE ma `percentage` - liczymy je z count/total w mapperze.
@JsonClass(generateAdapter = true)
data class SurveyResponseV3(
    @field:Json(name = "question") val question: String,
    @field:Json(name = "answers") val answers: List<SurveyAnswerResponseV3>,
    @field:Json(name = "count") val count: Int?,
    @field:Json(name = "voted") val voted: Int?,
)

@JsonClass(generateAdapter = true)
data class SurveyAnswerResponseV3(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "text") val text: String,
    @field:Json(name = "count") val count: Int,
    @field:Json(name = "voted") val voted: Int?,
)

package io.github.wykopmobilny.api.responses.v3.reports

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Odpowiedz POST /v3/reports/reports - adres formularza zgloszenia z serwerowym
 * hashem. Frontend wykop.pl przechodzi dokladnie pod ten adres (data.url).
 */
@JsonClass(generateAdapter = true)
data class ReportResponseV3(
    @field:Json(name = "url") val url: String,
)

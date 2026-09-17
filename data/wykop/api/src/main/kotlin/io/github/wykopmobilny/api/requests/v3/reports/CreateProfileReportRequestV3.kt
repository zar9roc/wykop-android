package io.github.wykopmobilny.api.requests.v3.reports

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Zgloszenie profilu: POST /v3/reports/reports z type="profile". API v3 nie ma
 * numerycznego id uzytkownika - profil identyfikuje `username`, wiec `id` jest
 * tu stringiem (tresci - wpisy, komentarze, znaleziska - maja id liczbowe,
 * patrz CreateReportRequestV3).
 */
@JsonClass(generateAdapter = true)
data class CreateProfileReportRequestV3(
    @field:Json(name = "id") val username: String,
    @field:Json(name = "type") val type: String = "profile",
)

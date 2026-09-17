package io.github.wykopmobilny.api.requests.v3.reports

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Zgloszenie tresci: POST /v3/reports/reports. `type` to rodzaj zasobu
 * (entry, entry_comment, link, link_comment...), `parent_id` dotyczy komentarzy
 * (id wpisu/znaleziska, pod ktorym stoi komentarz) - dla tresci "korzeniowych"
 * pomijane, dokladnie jak robi to frontend wykop.pl.
 */
@JsonClass(generateAdapter = true)
data class CreateReportRequestV3(
    @field:Json(name = "type") val type: String,
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "parent_id") val parentId: Long? = null,
)

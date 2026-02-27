package io.github.wykopmobilny.api.responses.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActionsResponseV3(
    @field:Json(name = "update") val update: Boolean?,
    @field:Json(name = "delete") val delete: Boolean?,
    @field:Json(name = "vote_up") val voteUp: Boolean?,
    @field:Json(name = "vote_down") val voteDown: Boolean?,
    @field:Json(name = "comment") val comment: Boolean?,
    @field:Json(name = "report") val report: Boolean?,
    @field:Json(name = "favourite") val favourite: Boolean?,
    @field:Json(name = "survey_vote") val surveyVote: Boolean?,
)

package io.github.wykopmobilny.api.responses.v3.common

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.ApiResponse
import io.github.wykopmobilny.api.responses.WykopErrorResponse

@JsonClass(generateAdapter = true)
data class WykopApiResponseV3<out T>(
    @field:Json(name = "data") override val data: T?,
    @field:Json(name = "pagination") val pagination: PaginationResponseV3?,
) : ApiResponse<T> {
    override val error: WykopErrorResponse? = null
}

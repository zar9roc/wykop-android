package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import retrofit2.http.GET
import retrofit2.http.Query

interface HitsV3RetrofitApi {
    @GET("v3/hits/links")
    suspend fun getHits(
        @Query("page") page: String? = null,
        @Query("sort") sort: String,
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>
}

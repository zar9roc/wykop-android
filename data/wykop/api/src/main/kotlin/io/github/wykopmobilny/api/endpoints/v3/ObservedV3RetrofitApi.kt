package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import retrofit2.http.GET
import retrofit2.http.Query

interface ObservedV3RetrofitApi {
    @GET("v3/observed/all")
    suspend fun getObservedAll(
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<ObservedItemV3>>
}

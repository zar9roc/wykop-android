package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.favourites.FavouriteRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Query

interface FavouritesV3RetrofitApi {
    @GET("v3/favourites")
    suspend fun getFavourites(
        @Query("page") page: String? = null,
        @Query("limit") limit: Int? = null,
    ): WykopApiResponseV3<List<ObservedItemV3>>

    @POST("v3/favourites")
    suspend fun addFavourite(
        @Body request: WykopApiRequestV3<FavouriteRequestV3>,
    ): WykopApiResponseV3<Unit>

    @HTTP(method = "DELETE", path = "v3/favourites", hasBody = true)
    suspend fun removeFavourite(
        @Body request: WykopApiRequestV3<FavouriteRequestV3>,
    ): WykopApiResponseV3<Unit>
}

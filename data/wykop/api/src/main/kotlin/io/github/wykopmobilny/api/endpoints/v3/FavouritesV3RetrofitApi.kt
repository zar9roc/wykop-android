package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.favourites.FavouriteRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

interface FavouritesV3RetrofitApi {
    @POST("v3/favourites")
    suspend fun addFavourite(
        @Body request: WykopApiRequestV3<FavouriteRequestV3>,
    ): WykopApiResponseV3<Unit>

    @HTTP(method = "DELETE", path = "v3/favourites", hasBody = true)
    suspend fun removeFavourite(
        @Body request: WykopApiRequestV3<FavouriteRequestV3>,
    ): WykopApiResponseV3<Unit>
}

package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserMeResponseV3
import retrofit2.http.GET
import retrofit2.http.Path

interface UsersV3RetrofitApi {
    @GET("/v3/profile/short")
    suspend fun getUserProfile(): WykopApiResponseV3<UserMeResponseV3>

    @GET("/v3/profile/users/{username}")
    suspend fun getUserFullProfile(
        @Path("username") username: String,
    ): WykopApiResponseV3<UserFullResponseV3>
}

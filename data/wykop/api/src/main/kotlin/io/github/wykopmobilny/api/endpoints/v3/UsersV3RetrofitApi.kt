package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserMeResponseV3
import retrofit2.http.GET

interface UsersV3RetrofitApi {
    @GET("/v3/users/me")
    suspend fun getUserProfile(): WykopApiResponseV3<UserMeResponseV3>
}

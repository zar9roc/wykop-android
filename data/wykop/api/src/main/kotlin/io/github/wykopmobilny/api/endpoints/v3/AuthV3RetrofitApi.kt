package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.api.requests.v3.auth.RefreshTokenRequestV3
import io.github.wykopmobilny.api.responses.v3.auth.AuthResponseV3
import io.github.wykopmobilny.api.responses.v3.auth.ConnectResponseV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthV3RetrofitApi {
    @POST("v3/auth")
    suspend fun authenticate(
        @Body request: AuthRequestV3,
    ): WykopApiResponseV3<AuthResponseV3>

    @POST("v3/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequestV3,
    ): WykopApiResponseV3<AuthResponseV3>

    @GET("v3/connect")
    suspend fun connect(): WykopApiResponseV3<ConnectResponseV3>
}

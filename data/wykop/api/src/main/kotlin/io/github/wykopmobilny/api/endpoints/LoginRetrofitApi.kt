package io.github.wykopmobilny.api.endpoints

import io.github.wykopmobilny.APP_KEY
import io.github.wykopmobilny.REMOVE_USERKEY_HEADER
import io.github.wykopmobilny.api.responses.LoginResponse
import io.github.wykopmobilny.api.responses.TwoFactorAuthorizationResponse
import io.github.wykopmobilny.api.responses.WykopApiResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

@Deprecated("API v1/v2 is obsolete - server no longer responds to these endpoints. Migrate to v3 API.")
interface LoginRetrofitApi {
    @Headers("@: $REMOVE_USERKEY_HEADER")
    @FormUrlEncoded
    @POST("/login/index/appkey/$APP_KEY")
    suspend fun getUserSessionToken(
        @Field("login") login: String,
        @Field("accountkey", encoded = true) accountKey: String,
    ): WykopApiResponse<LoginResponse>

    @FormUrlEncoded
    @POST("/login/2fa/appkey/$APP_KEY")
    suspend fun autorizeWith2FA(
        @Field("code") code: String,
    ): WykopApiResponse<List<TwoFactorAuthorizationResponse>>
}

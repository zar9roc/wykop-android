package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.blacklist.BlacklistDomainRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.responses.v3.blacklist.BlacklistedDomainResponseV3
import io.github.wykopmobilny.api.responses.v3.blacklist.BlacklistedTagResponseV3
import io.github.wykopmobilny.api.responses.v3.blacklist.BlacklistedUserResponseV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BlacklistV3RetrofitApi {
    @GET("v3/settings/blacklists/users")
    suspend fun getBlacklistedUsers(
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<BlacklistedUserResponseV3>>

    @GET("v3/settings/blacklists/tags")
    suspend fun getBlacklistedTags(
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<BlacklistedTagResponseV3>>

    @GET("v3/settings/blacklists/domains")
    suspend fun getBlacklistedDomains(
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<BlacklistedDomainResponseV3>>

    // Analogicznie do users/tagow: sciezka /domains/{domain} zwraca 405 - blokada
    // przez kolekcje POST /domains z body {data:{domain}}. Usuniecie per-domain.
    @POST("v3/settings/blacklists/domains")
    suspend fun blockDomain(
        @Body request: WykopApiRequestV3<BlacklistDomainRequestV3>,
    ): Response<Unit>

    @DELETE("v3/settings/blacklists/domains/{domain}")
    suspend fun unblockDomain(
        @Path("domain") domain: String,
    ): Response<Unit>
}

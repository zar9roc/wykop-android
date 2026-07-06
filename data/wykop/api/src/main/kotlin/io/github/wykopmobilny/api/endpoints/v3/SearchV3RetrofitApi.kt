package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.http.GET
import retrofit2.http.Query

// Spec OpenAPI deklaruje parametr "q", ale serwer honoruje tylko "query" -
// z "q" zwraca niefiltrowany strumien (wyniki bez zwiazku z zapytaniem).
interface SearchV3RetrofitApi {
    @GET("v3/search/links")
    suspend fun searchLinks(
        @Query("query") query: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/search/entries")
    suspend fun searchEntries(
        @Query("query") query: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/search/users")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<UserShortResponseV3>>
}

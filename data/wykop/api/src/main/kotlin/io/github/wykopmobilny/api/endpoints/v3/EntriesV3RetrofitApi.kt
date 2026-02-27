package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EntriesV3RetrofitApi {
    @GET("/v3/entries/popular")
    suspend fun getHot(
        @Query("page") page: Int,
        @Query("sort") sort: String = "best",
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("/v3/entries/newest")
    suspend fun getStream(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("/v3/entries/active")
    suspend fun getActive(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("/v3/entries/observed")
    suspend fun getObserved(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("/v3/entries/{id}")
    suspend fun getEntry(
        @Path("id") id: Long,
    ): WykopApiResponseV3<EntryResponseV3>

    @GET("/v3/entries/{id}/votes/up")
    suspend fun getEntryVoters(
        @Path("id") id: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>

    @GET("/v3/entries/comments/{id}/votes/up")
    suspend fun getCommentUpvoters(
        @Path("id") id: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>
}

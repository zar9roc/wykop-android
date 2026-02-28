package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LinksV3RetrofitApi {
    @GET("v3/links")
    suspend fun getLinks(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/links/upcoming")
    suspend fun getUpcoming(
        @Query("page") page: Int,
        @Query("sort") sortBy: String,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/links/observed")
    suspend fun getObserved(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/links/{id}/comments")
    suspend fun getLinkComments(
        @Path("id") linkId: Long,
        @Query("sort") sortBy: String,
    ): WykopApiResponseV3<List<LinkCommentResponseV3>>

    @GET("v3/links/{id}")
    suspend fun getLink(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<LinkResponseV3>

    @GET("v3/links/{id}/votes/up")
    suspend fun getUpvoters(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>

    @GET("v3/links/{id}/votes/down")
    suspend fun getDownvoters(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>

    @GET("v3/links/{id}/related")
    suspend fun getRelated(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<List<RelatedResponseV3>>
}

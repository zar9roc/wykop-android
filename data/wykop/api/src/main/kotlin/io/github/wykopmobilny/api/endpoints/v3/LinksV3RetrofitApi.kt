package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateCommentRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LinksV3RetrofitApi {
    @GET("v3/links")
    suspend fun getLinks(
        @Query("page") page: String? = null,
        @Query("type") type: String? = null,
        @Query("sort") sort: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @Deprecated("Use getLinks(page, type = \"upcoming\", sort = sortBy) instead")
    @GET("v3/links/upcoming")
    suspend fun getUpcoming(
        @Query("page") page: String? = null,
        @Query("sort") sortBy: String,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/links/observed")
    suspend fun getObserved(
        @Query("page") page: String? = null,
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

    @POST("v3/links/{linkId}/comments")
    suspend fun addLinkComment(
        @Path("linkId") linkId: Long,
        @Body request: WykopApiRequestV3<CreateUpdateCommentRequestV3>,
    ): WykopApiResponseV3<LinkCommentResponseV3>

    @PUT("v3/links/{linkId}/comments/{commentId}")
    suspend fun editLinkComment(
        @Path("linkId") linkId: Long,
        @Path("commentId") commentId: Long,
        @Body request: WykopApiRequestV3<CreateUpdateCommentRequestV3>,
    ): WykopApiResponseV3<Unit>
}

package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateCommentRequestV3
import io.github.wykopmobilny.api.requests.v3.links.AddRelatedRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkVoterResponseV3
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @Deprecated(
        "Endpoint /v3/links/observed does not exist in API v3. " +
            "Use FavouritesV3RetrofitApi.getFavourites() instead, " +
            "which returns both links and entries.",
        ReplaceWith("favouritesApiV3.getFavourites(page)"),
    )
    @GET("v3/links/observed")
    suspend fun getObserved(
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/links/{id}/comments")
    suspend fun getLinkComments(
        @Path("id") linkId: Long,
        @Query("sort") sortBy: String,
        // null = pierwsza strona (regula paginacji API v3), potem 2, 3...
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<LinkCommentResponseV3>>

    @GET("v3/links/{linkId}/comments/{commentId}/comments")
    suspend fun getLinkCommentReplies(
        @Path("linkId") linkId: Long,
        @Path("commentId") commentId: Long,
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<LinkCommentResponseV3>>

    @GET("v3/links/{linkId}/comments/{commentId}")
    suspend fun getLinkComment(
        @Path("linkId") linkId: Long,
        @Path("commentId") commentId: Long,
    ): WykopApiResponseV3<LinkCommentResponseV3>

    @GET("v3/links/{id}")
    suspend fun getLink(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<LinkResponseV3>

    // Lista glosujacych to GET /links/{id}/upvotes/{type} (up|down) -
    // sciezki /votes/up i /votes/down/{reason} sa TYLKO do oddawania glosow (POST).
    @GET("v3/links/{id}/upvotes/up")
    suspend fun getUpvoters(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<List<LinkVoterResponseV3>>

    @GET("v3/links/{id}/upvotes/down")
    suspend fun getDownvoters(
        @Path("id") linkId: Long,
    ): WykopApiResponseV3<List<LinkVoterResponseV3>>

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

    @DELETE("v3/links/{linkId}/comments/{commentId}")
    suspend fun deleteLinkComment(
        @Path("linkId") linkId: Long,
        @Path("commentId") commentId: Long,
    ): WykopApiResponseV3<Unit>?

    // region Link votes

    @POST("v3/links/{linkId}/votes/up")
    suspend fun voteUp(
        @Path("linkId") linkId: Long,
    ): Response<Unit>

    @POST("v3/links/{linkId}/votes/down/{reason}")
    suspend fun voteDown(
        @Path("linkId") linkId: Long,
        @Path("reason") reason: Int,
    ): Response<Unit>

    @DELETE("v3/links/{linkId}/votes")
    suspend fun removeVote(
        @Path("linkId") linkId: Long,
    ): Response<Unit>

    // endregion

    // region Link comment votes

    @POST("v3/links/{linkId}/comments/{commentId}/votes/{type}")
    suspend fun voteComment(
        @Path("linkId") linkId: Long,
        @Path("commentId") commentId: Long,
        @Path("type") type: String,
    ): Response<Unit>

    @DELETE("v3/links/{linkId}/comments/{commentId}/votes")
    suspend fun removeCommentVote(
        @Path("linkId") linkId: Long,
        @Path("commentId") commentId: Long,
    ): Response<Unit>

    // endregion

    // region Related links

    @POST("v3/links/{linkId}/related")
    suspend fun addRelated(
        @Path("linkId") linkId: Long,
        @Body request: WykopApiRequestV3<AddRelatedRequestV3>,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/links/{linkId}/related/{relatedId}")
    suspend fun deleteRelated(
        @Path("linkId") linkId: Long,
        @Path("relatedId") relatedId: Long,
    ): WykopApiResponseV3<Unit>?

    @POST("v3/links/{linkId}/related/{relatedId}/votes/{type}")
    suspend fun voteRelated(
        @Path("linkId") linkId: Long,
        @Path("relatedId") relatedId: Long,
        @Path("type") type: String,
    ): Response<Unit>

    @DELETE("v3/links/{linkId}/related/{relatedId}/votes")
    suspend fun removeRelatedVote(
        @Path("linkId") linkId: Long,
        @Path("relatedId") relatedId: Long,
    ): Response<Unit>

    // endregion

    // region Observed discussions

    @POST("v3/links/{linkId}/observed-discussions")
    suspend fun observeDiscussions(
        @Path("linkId") linkId: Long,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/links/{linkId}/observed-discussions")
    suspend fun unobserveDiscussions(
        @Path("linkId") linkId: Long,
    ): WykopApiResponseV3<Unit>?

    // endregion
}

package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileV3RetrofitApi {
    @GET("v3/profile")
    suspend fun getProfile(): WykopApiResponseV3<UserFullResponseV3>

    @GET("v3/profile/short")
    suspend fun getProfileShort(): WykopApiResponseV3<UserShortResponseV3>

    @GET("v3/profile/users/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String,
    ): WykopApiResponseV3<UserFullResponseV3>

    @GET("v3/profile/users/{username}/short")
    suspend fun getUserProfileShort(
        @Path("username") username: String,
    ): WykopApiResponseV3<UserShortResponseV3>

    @GET("v3/profile/users/{username}/entries/added")
    suspend fun getUserEntriesAdded(
        @Path("username") username: String,
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/profile/users/{username}/entries/voted")
    suspend fun getUserEntriesVoted(
        @Path("username") username: String,
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/profile/users/{username}/entries/commented")
    suspend fun getUserEntriesCommented(
        @Path("username") username: String,
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryCommentResponseV3>>

    @GET("v3/profile/users/{username}/links/added")
    suspend fun getUserLinksAdded(
        @Path("username") username: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/profile/users/{username}/links/published")
    suspend fun getUserLinksPublished(
        @Path("username") username: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/profile/users/{username}/links/up")
    suspend fun getUserLinksUp(
        @Path("username") username: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/profile/users/{username}/links/down")
    suspend fun getUserLinksDown(
        @Path("username") username: String,
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/profile/users/{username}/links/commented")
    suspend fun getUserLinksCommented(
        @Path("username") username: String,
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<LinkCommentResponseV3>>

    @GET("v3/profile/users/{username}/links/related")
    suspend fun getUserLinksRelated(
        @Path("username") username: String,
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<RelatedResponseV3>>

    @GET("v3/profile/users/{username}/actions")
    suspend fun getUserActions(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/profile/users/{username}/badges")
    suspend fun getUserBadges(
        @Path("username") username: String,
    ): WykopApiResponseV3<List<BadgeResponseV3>>

    @POST("v3/observed/users/{username}")
    suspend fun observeUser(
        @Path("username") username: String,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/observed/users/{username}")
    suspend fun unobserveUser(
        @Path("username") username: String,
    ): WykopApiResponseV3<Unit>

    @POST("v3/settings/blacklists/users/{username}")
    suspend fun blockUser(
        @Path("username") username: String,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/settings/blacklists/users/{username}")
    suspend fun unblockUser(
        @Path("username") username: String,
    ): WykopApiResponseV3<Unit>
}

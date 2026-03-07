package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.tags.TagDetailsResponseV3
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TagsV3RetrofitApi {
    @GET("v3/tags/{tagName}/stream")
    suspend fun getTagEntries(
        @Path("tagName") tagName: String,
        @Query("page") page: String? = null,
        @Query("sort") sort: String? = null,
        @Query("type") type: String = "entry",
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/tags/{tagName}/stream")
    suspend fun getTagLinks(
        @Path("tagName") tagName: String,
        @Query("page") page: String? = null,
        @Query("sort") sort: String? = null,
        @Query("type") type: String = "link",
    ): WykopApiResponseV3<List<LinkResponseV3>>

    @GET("v3/tags/{tagName}")
    suspend fun getTagDetails(
        @Path("tagName") tagName: String,
    ): WykopApiResponseV3<TagDetailsResponseV3>
}

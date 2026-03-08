package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.suggest.TagSuggestionResponseV3
import io.github.wykopmobilny.api.responses.v3.suggest.UserSuggestionResponseV3
import retrofit2.http.GET
import retrofit2.http.Query

interface SuggestV3RetrofitApi {
    @GET("v3/tags/autocomplete")
    suspend fun getTagSuggestions(
        @Query("query") query: String,
    ): WykopApiResponseV3<List<TagSuggestionResponseV3>>

    @GET("v3/users/autocomplete")
    suspend fun getUserSuggestions(
        @Query("query") query: String,
    ): WykopApiResponseV3<List<UserSuggestionResponseV3>>
}

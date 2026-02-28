package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

enum class EntriesSort {
    HOT,
    ACTIVE,
    NEWEST,
    ;

    override fun toString(): String =
        when (this) {
            HOT -> "hot"
            ACTIVE -> "active"
            NEWEST -> "newest"
        }
}

enum class EntriesLastUpdate(
    val hours: Int,
) {
    ONE_HOUR(1),
    TWO_HOURS(2),
    THREE_HOURS(3),
    SIX_HOURS(6),
    TWELVE_HOURS(12),
    TWENTY_FOUR_HOURS(24),
    ;

    override fun toString(): String = hours.toString()
}

interface EntriesV3RetrofitApi {
    @GET("v3/entries")
    suspend fun getEntries(
        @Query("page") page: Int,
        @Query("sort") sort: EntriesSort = EntriesSort.HOT,
        @Query("last_update") lastUpdate: EntriesLastUpdate? = null,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/entries/observed")
    suspend fun getObserved(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/entries/{id}")
    suspend fun getEntry(
        @Path("id") id: Long,
    ): WykopApiResponseV3<EntryResponseV3>

    @GET("v3/entries/{id}/votes/up")
    suspend fun getEntryVoters(
        @Path("id") id: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>

    @GET("v3/entries/comments/{id}/votes/up")
    suspend fun getCommentUpvoters(
        @Path("id") id: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>
}

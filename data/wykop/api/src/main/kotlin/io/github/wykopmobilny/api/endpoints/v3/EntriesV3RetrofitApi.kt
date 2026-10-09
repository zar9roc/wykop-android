package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateSurveyRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateThreadCommentRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateCommentRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateEntryRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.VoteSurveyRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.CreateSurveyResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.ThreadAncestorResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.ThreadSubcommentsResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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
        @Query("page") page: String? = null,
        @Query("sort") sort: EntriesSort = EntriesSort.HOT,
        @Query("last_update") lastUpdate: EntriesLastUpdate? = null,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/observed/tags/stream")
    suspend fun getObservedTagsStream(
        @Query("page") page: String? = null,
    ): WykopApiResponseV3<List<EntryResponseV3>>

    @GET("v3/entries/{entryId}")
    suspend fun getEntry(
        @Path("entryId") entryId: Long,
    ): WykopApiResponseV3<EntryResponseV3>

    @GET("v3/entries/{entryId}/votes")
    suspend fun getEntryVoters(
        @Path("entryId") entryId: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>

    @GET("v3/entries/{entryId}/comments")
    suspend fun getEntryComments(
        @Path("entryId") entryId: Long,
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<EntryCommentResponseV3>>

    /**
     * Pojedynczy komentarz z watku - opcjonalnie od razu z kawalkiem poddrzewa.
     *
     * `comments_expanded=true` dociaga TRZY poziomy w dol od komentarza z URL-a:
     * do `comments_limit` odpowiedzi poziomu `nesting+1`, z czego pierwsze 3 dostaja
     * po 2 odpowiedzi `nesting+2`, a kazda z nich po 1 odpowiedzi `nesting+3`.
     * Reszta ma `comments.items` puste, ale `comments.total` zawsze prawdziwe -
     * po tej roznicy poznajemy galezie do dopytania.
     *
     * `comments_sort` musi byc spojny z `sort` uzywanym w [getThreadSubcomments],
     * inaczej sklejanie ekspansji z dopytaniami rozjedzie kolejnosc i zdubluje wyniki.
     */
    @GET("v3/entries-threads/{entryId}/comments/{commentId}")
    suspend fun getThreadComment(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
        @Query("comments_sort") commentsSort: String = THREAD_SORT_OLDEST,
        @Query("comments_limit") commentsLimit: Int = 0,
        @Query("comments_expanded") commentsExpanded: Boolean = false,
    ): WykopApiResponseV3<ThreadAncestorResponseV3>

    /**
     * Odpowiedz zagniezdzona w watku - POST pod komentarz-rodzic, nie pod wpis.
     * Zwykle POST /v3/entries/{id}/comments tworzy komentarz pierwszego poziomu,
     * ten endpoint podwiesza go pod wskazany komentarz.
     */
    @POST("v3/entries-threads/{entryId}/comments/{parentCommentId}/comments")
    suspend fun addThreadReply(
        @Path("entryId") entryId: Long,
        @Path("parentCommentId") parentCommentId: Long,
        @Body request: WykopApiRequestV3<CreateThreadCommentRequestV3>,
    ): WykopApiResponseV3<ThreadAncestorResponseV3>

    /**
     * Sciezka przodkow komentarza: wpis + kolejni rodzice az do bezposredniego,
     * bez samego komentarza. Jedno zapytanie niezaleznie od glebokosci watku.
     * Uwaga: `entryId` musi pasowac do komentarza, inaczej API zwraca 404.
     */
    @GET("v3/entries-threads/{entryId}/comments/{commentId}/ancestors")
    suspend fun getCommentAncestors(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
    ): WykopApiResponseV3<List<ThreadAncestorResponseV3>>

    /**
     * Bezposrednie odpowiedzi pod komentarzem watku. Paginacja jest KURSOROWA:
     * przy `sort=oldest` parametr `id` wyklucza podany element i zwraca kolejne
     * o wiekszym id (rosnaco), wiec cala kolekcje przechodzi sie podajac id
     * ostatniego elementu poprzedniej strony. `sort=best` dokleja do strony
     * elementy nadmiarowe - tutaj go nie uzywamy.
     *
     * `data` to obiekt z `items` + `count`/`total` (liczba wszystkich odpowiedzi
     * tego komentarza), a nie gola lista.
     *
     * `expanded=true` dociaga trzy poziomy w dol (ta sama, niepelna ekspansja co
     * `comments_expanded` w [getThreadComment]) - uzywamy go, bo jedno zapytanie
     * zalatwia wtedy znacznie wiekszy kawalek poddrzewa.
     */
    @GET("v3/entries-threads/{entryId}/comments/{commentId}/comments")
    suspend fun getThreadSubcomments(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
        @Query("sort") sort: String = THREAD_SORT_OLDEST,
        @Query("id") cursor: Long? = null,
        @Query("limit") limit: Int = THREAD_MAX_LIMIT,
        @Query("expanded") expanded: Boolean = false,
    ): WykopApiResponseV3<ThreadSubcommentsResponseV3>

    @GET("v3/entries/{entryId}/comments/{commentId}/votes")
    suspend fun getCommentVoters(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
    ): WykopApiResponseV3<List<UserShortResponseV3>>

    // Write operations
    @POST("v3/entries")
    suspend fun addEntry(
        @Body request: WykopApiRequestV3<CreateUpdateEntryRequestV3>,
    ): WykopApiResponseV3<EntryResponseV3>

    // Tworzy ankiete (pytanie + odpowiedzi), zwraca survey_id do doklejenia do wpisu.
    @POST("v3/entries/survey")
    suspend fun createSurvey(
        @Body request: WykopApiRequestV3<CreateSurveyRequestV3>,
    ): WykopApiResponseV3<CreateSurveyResponseV3>

    @PUT("v3/entries/{entryId}")
    suspend fun editEntry(
        @Path("entryId") entryId: Long,
        @Body request: WykopApiRequestV3<CreateUpdateEntryRequestV3>,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/entries/{entryId}")
    suspend fun deleteEntry(
        @Path("entryId") entryId: Long,
    ): Response<Unit>

    @POST("v3/entries/{entryId}/votes")
    suspend fun voteEntry(
        @Path("entryId") entryId: Long,
    ): Response<Unit>

    @DELETE("v3/entries/{entryId}/votes")
    suspend fun unvoteEntry(
        @Path("entryId") entryId: Long,
    ): Response<Unit>

    // Obserwowanie dyskusji we wpisie (powiadomienia o nowych komentarzach) - 204 bez body.
    @POST("v3/entries/{entryId}/observed-discussions")
    suspend fun observeDiscussion(
        @Path("entryId") entryId: Long,
    ): Response<Unit>

    @DELETE("v3/entries/{entryId}/observed-discussions")
    suspend fun unobserveDiscussion(
        @Path("entryId") entryId: Long,
    ): Response<Unit>

    @POST("v3/entries/{entryId}/comments")
    suspend fun addEntryComment(
        @Path("entryId") entryId: Long,
        @Body request: WykopApiRequestV3<CreateUpdateCommentRequestV3>,
    ): WykopApiResponseV3<EntryCommentResponseV3>

    @PUT("v3/entries/{entryId}/comments/{commentId}")
    suspend fun editEntryComment(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
        @Body request: WykopApiRequestV3<CreateUpdateCommentRequestV3>,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/entries/{entryId}/comments/{commentId}")
    suspend fun deleteEntryComment(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
    ): Response<Unit>

    @POST("v3/entries/{entryId}/comments/{commentId}/votes")
    suspend fun voteComment(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
    ): Response<Unit>

    @DELETE("v3/entries/{entryId}/comments/{commentId}/votes")
    suspend fun unvoteComment(
        @Path("entryId") entryId: Long,
        @Path("commentId") commentId: Long,
    ): Response<Unit>

    @POST("v3/entries/{entryId}/survey/votes")
    suspend fun voteSurvey(
        @Path("entryId") entryId: Long,
        @Body request: WykopApiRequestV3<VoteSurveyRequestV3>,
    ): Response<Unit>

    companion object {
        /** Jedyny tryb z prostym, wykluczajacym kursorem po `id` (rosnaco). */
        const val THREAD_SORT_OLDEST = "oldest"

        /** Gorny limit strony odpowiedzi w watku wedlug specyfikacji API. */
        const val THREAD_MAX_LIMIT = 25

        /** Watek nie schodzi glebiej - komentarz o tym `nesting` nie ma juz dzieci. */
        const val THREAD_MAX_NESTING = 7
    }
}

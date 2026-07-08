package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.notes.NoteRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.notes.NoteResponseV3
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotesV3RetrofitApi {
    @GET("v3/notes")
    suspend fun getNotes(
        @Query("page") page: Int? = null,
    ): WykopApiResponseV3<List<NoteResponseV3>>

    @GET("v3/notes/{username}")
    suspend fun getNote(
        @Path("username") username: String,
    ): WykopApiResponseV3<NoteResponseV3>

    // Zapis i usuwanie idzie tym samym PUT: pusta tresc = usuniecie (notatka znika
    // z listy /notes). DELETE /notes/{username} zwraca 405 (spec klamie).
    @PUT("v3/notes/{username}")
    suspend fun saveNote(
        @Path("username") username: String,
        @Body request: WykopApiRequestV3<NoteRequestV3>,
    ): Response<Unit>
}

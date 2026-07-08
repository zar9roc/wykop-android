package io.github.wykopmobilny.api.notes

import io.github.wykopmobilny.api.endpoints.v3.NotesV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.notes.NoteRequestV3
import javax.inject.Inject

class NotesRepository
    @Inject
    constructor(
        private val notesApi: NotesV3RetrofitApi,
    ) {
        // Pusta tresc = brak notatki.
        suspend fun getNote(username: String): String? =
            notesApi.getNote(username).data?.content?.takeIf { it.isNotBlank() }

        // Zapis; pusta tresc usuwa notatke (znika z listy /notes). DELETE /notes/{username}
        // zwraca 405, wiec usuwanie tez idzie przez PUT z pusta trescia.
        suspend fun saveNote(
            username: String,
            content: String,
        ) {
            notesApi.saveNote(username, WykopApiRequestV3(NoteRequestV3(content = content.trim())))
        }
    }

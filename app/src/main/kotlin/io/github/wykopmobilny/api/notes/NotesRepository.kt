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
        // Pusta tresc = brak notatki. Przy okazji synchronizujemy [NoteOverrideCache]:
        // - content niepusty -> notatka istnieje, kasujemy override (samonaprawa, np. gdy
        //   notatka dodana poza aplikacja);
        // - content pusty a API zwraca user.note=true -> bezposredni dowod buga API
        //   (flaga nie wyczyszczona po usunieciu) -> zapamietujemy override "brak notatki".
        suspend fun getNote(username: String): String? {
            val data = notesApi.getNote(username).data
            val content = data?.content?.takeIf { it.isNotBlank() }
            if (content != null) {
                NoteOverrideCache.clearNote(username)
            } else if (data?.user?.note == true) {
                NoteOverrideCache.markNoNote(username)
            } else {
                NoteOverrideCache.clearNote(username)
            }
            return content
        }

        // Zapis; pusta tresc usuwa notatke (znika z listy /notes). DELETE /notes/{username}
        // zwraca 405, wiec usuwanie tez idzie przez PUT z pusta trescia. Aktualizujemy tez
        // override: pusta tresc -> "brak notatki", niepusta -> kasujemy override.
        suspend fun saveNote(
            username: String,
            content: String,
        ) {
            val trimmed = content.trim()
            notesApi.saveNote(username, WykopApiRequestV3(NoteRequestV3(content = trimmed)))
            if (trimmed.isEmpty()) {
                NoteOverrideCache.markNoNote(username)
            } else {
                NoteOverrideCache.clearNote(username)
            }
        }
    }

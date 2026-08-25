package io.github.wykopmobilny.api.notes

import io.github.wykopmobilny.data.cache.api.ApplicationCache
import java.util.concurrent.ConcurrentHashMap

/**
 * Lokalny override statusu notatki - obejscie buga API v3, ktore po usunieciu notatki
 * nadal zwraca `note=true` na obiekcie autora. Login obecny w zbiorze = wiemy, ze user
 * NIE ma notatki, wiec zolta kartka ma byc ukryta niezaleznie od flagi z backendu.
 *
 * Zbior w pamieci (odczyt synchroniczny z mapperow, m.in. [io.github.wykopmobilny.models
 * .mapper.apiv3.AuthorMapperV3]) z trwalym backingiem w tabeli noteOverrideEntity, wiec
 * przezywa refetch i restart aplikacji. Zapisy centralnie w [NotesRepository].
 */
object NoteOverrideCache {
    @Volatile
    private var applicationCache: ApplicationCache? = null
    private val noNote = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var loaded = false

    fun init(cache: ApplicationCache) {
        applicationCache = cache
    }

    private fun ensureLoaded() {
        if (loaded) return
        val cache = applicationCache ?: return
        synchronized(this) {
            if (loaded) return
            runCatching {
                cache.cache().noteOverrideQueries.selectAll().executeAsList().forEach(noNote::add)
            }
            loaded = true
        }
    }

    /** true = wiemy lokalnie, ze user nie ma notatki (mimo ewentualnego note=true z API). */
    fun hasNoNote(username: String): Boolean {
        ensureLoaded()
        return noNote.contains(username)
    }

    /** Zapamietaj, ze user nie ma notatki (usunieta / dowod buga API). */
    fun markNoNote(username: String) {
        ensureLoaded()
        if (noNote.add(username)) {
            runCatching { applicationCache?.cache()?.noteOverrideQueries?.put(username) }
        }
    }

    /** Notatka faktycznie istnieje - kasujemy override (samonaprawa). */
    fun clearNote(username: String) {
        ensureLoaded()
        if (noNote.remove(username)) {
            runCatching { applicationCache?.cache()?.noteOverrideQueries?.remove(username) }
        }
    }
}

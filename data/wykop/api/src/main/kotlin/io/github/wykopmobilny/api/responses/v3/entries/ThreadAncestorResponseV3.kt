package io.github.wykopmobilny.api.responses.v3.entries

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import kotlinx.datetime.Instant

/**
 * Element watku z /v3/entries-threads: wpis (`resource` = "entry") albo komentarz
 * ("entry_comment"). Oba maja ten sam ksztalt, wiec opisuje je jeden model.
 *
 * Uzywany w trzech miejscach:
 * - GET .../comments/{id}/ancestors - sciezka od wpisu do bezposredniego rodzica,
 * - GET .../comments/{id} - sam komentarz (z `comments_expanded=true` takze z poddrzewem),
 * - GET .../comments/{id}/comments - strona odpowiedzi (`items`).
 */
@JsonClass(generateAdapter = true)
data class ThreadAncestorResponseV3(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "resource") val resource: String,
    @field:Json(name = "author") val author: UserShortResponseV3,
    @field:Json(name = "created_at") val createdAt: Instant,
    @field:Json(name = "content") val content: String?,
    @field:Json(name = "slug") val slug: String?,
    @field:Json(name = "votes") val votes: VotesResponseV3?,
    @field:Json(name = "voted") val voted: Int?,
    @field:Json(name = "favourite") val favourite: Boolean?,
    @field:Json(name = "adult") val adult: Boolean?,
    @field:Json(name = "media") val media: MediaResponseV3?,
    @field:Json(name = "device") val device: String?,
    @field:Json(name = "observed_discussion") val observedDiscussion: Boolean?,
    // Poziom zagniezdzenia liczony od wpisu (wpis = 1, komentarz pierwszego poziomu = 2).
    @field:Json(name = "nesting") val nesting: Int?,
    // UWAGA: typ pola zalezy od rodzaju elementu - komentarz ma null albo powod
    // usuniecia ("moderator"/"author"/"host"), a wpis dostaje boolean `false`.
    // Zadeklarowane jako String? wywalalo parsowanie calej odpowiedzi
    // (JsonDataException: Expected a string but was BOOLEAN at $.data[0].deleted).
    @field:Json(name = "deleted") val deleted: Any?,
    @field:Json(name = "comments") val comments: ThreadCountsResponseV3?,
) {
    val isEntry: Boolean
        get() = resource == RESOURCE_ENTRY

    /** Powod usuniecia; boolean `false` przy wpisie znaczy "nieusuniety". */
    val deletedReason: String?
        get() = (deleted as? String)?.takeIf { it.isNotEmpty() }

    /**
     * Liczba WSZYSTKICH bezposrednich odpowiedzi wedlug API - zawsze policzona
     * poprawnie, niezaleznie od tego ile odpowiedzi przyszlo w `items`.
     * `null` znaczy "API nie podalo licznika", a nie "zero".
     */
    val replyTotal: Int?
        get() = comments?.total ?: comments?.count

    /** Odpowiedzi faktycznie przyslane w tej odpowiedzi HTTP (ekspansja bywa niepelna). */
    val expandedReplies: List<ThreadAncestorResponseV3>
        get() = comments?.items.orEmpty()

    companion object {
        const val RESOURCE_ENTRY = "entry"
    }
}

/**
 * Licznik odpowiedzi przy elemencie watku razem z juz doczytanymi odpowiedziami.
 *
 * `count`/`total` to liczba WSZYSTKICH bezposrednich odpowiedzi o poziom nizej -
 * API liczy je poprawnie zawsze, niezaleznie od ekspansji. `items` wypelnia sie
 * dopiero przy `comments_expanded=true` (endpoint komentarza) albo `expanded=true`
 * (endpoint odpowiedzi) i jest NIEPELNE z zalozenia: ekspansja schodzi trzy poziomy
 * w dol, ale tylko pierwsze 3 odpowiedzi poziomu nizej dostaja po 2 wnuki, a te po
 * 1 prawnuku. Rozjazd `items.size` < `total` jest wiec normalny i sluzy nam za
 * wskaznik "tu trzeba dopytac".
 *
 * Typ jest REKURENCYJNY (ThreadAncestorResponseV3 -> ThreadCountsResponseV3 ->
 * List<ThreadAncestorResponseV3>). Moshi to lyka - przy cyklu w grafie adapterow
 * `Moshi.adapter` zwraca odroczony adapter i domyka go po zbudowaniu calego lancucha.
 */
@JsonClass(generateAdapter = true)
data class ThreadCountsResponseV3(
    @field:Json(name = "count") val count: Int? = null,
    @field:Json(name = "total") val total: Int? = null,
    @field:Json(name = "items") val items: List<ThreadAncestorResponseV3>? = null,
)

/**
 * Odpowiedzi pod komentarzem z GET .../comments/{id}/comments. `data` jest tu
 * OBIEKTEM (a nie gola lista jak przy `ancestors`): `items` to strona wynikow,
 * a `total` liczba WSZYSTKICH bezposrednich odpowiedzi - niezalezna od `limit`
 * i kursora, wiec po niej poznajemy koniec kolekcji.
 */
@JsonClass(generateAdapter = true)
data class ThreadSubcommentsResponseV3(
    @field:Json(name = "items") val items: List<ThreadAncestorResponseV3>? = null,
    @field:Json(name = "count") val count: Int? = null,
    @field:Json(name = "total") val total: Int? = null,
)

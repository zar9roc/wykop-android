package io.github.wykopmobilny.api.responses.v3.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserShortResponseV3(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "avatar") val avatar: String?,
    @field:Json(name = "color") val color: String?,
    // "active" | "banned" | "removed" - zbanowani/usunieci maja w `color` dalej
    // swoj rangowy kolor, wiec status jest jedynym zrodlem szarego nicka.
    @field:Json(name = "status") val status: String?,
    @field:Json(name = "gender") val gender: String?,
    @field:Json(name = "verified") val verified: Boolean?,
    @field:Json(name = "sponsor") val sponsor: Boolean?,
    @field:Json(name = "online") val online: Boolean?,
    // Czy zalogowany uzytkownik ma notatke o tym autorze (tresc dociagana z /notes/{username}).
    @field:Json(name = "note") val note: Boolean?,
)

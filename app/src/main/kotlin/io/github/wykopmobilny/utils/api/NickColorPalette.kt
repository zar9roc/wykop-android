package io.github.wykopmobilny.utils.api

import android.graphics.Color

/**
 * Kolory rang nicków pobierane z /v3/config (raz na tydzień, patrz WykopApp) - jeden
 * hex dla trybu jasnego, drugi dla nocnego. Wartości podane przez wykop jako fallback,
 * dopóki (albo gdyby) config się nie pobrał. Proces-globalny cache czytany przez
 * getGroupColor; aktualizowany po pobraniu i seedowany z prefs przy starcie.
 */
object NickColorPalette {
    data class Hex(
        val light: String,
        val dark: String,
    )

    // Fallback = wartości z zadania (obecna odpowiedź /v3/config).
    private val fallback =
        mapOf(
            "black" to Hex(light = "000000", dark = "ffffff"),
            "burgundy" to Hex(light = "990000", dark = "bb1111"),
            "green" to Hex(light = "339933", dark = "339933"),
            "orange" to Hex(light = "ff5917", dark = "ff5917"),
            "purple" to Hex(light = "593787", dark = "694797"),
            "red" to Hex(light = "d81e04", dark = "d81e04"),
        )

    @Volatile
    private var colors: Map<String, Hex> = fallback

    /** Nadpisuje kolory z configu; brakujące nazwy zostają na fallbacku. */
    fun apply(newColors: Map<String, Hex>) {
        if (newColors.isNotEmpty()) {
            colors = fallback + newColors
        }
    }

    fun colorInt(
        name: String?,
        isDark: Boolean,
    ): Int? {
        val hex = colors[name] ?: return null
        return runCatching { Color.parseColor("#${if (isDark) hex.dark else hex.light}") }.getOrNull()
    }
}

// Odwrotność colorNameToGroupId - grupy 0..5 odpowiadają nazwom kolorów z /v3/config.
fun groupIdToColorName(role: Int): String? =
    when (role) {
        0 -> "green"
        1 -> "orange"
        2 -> "burgundy"
        3 -> "purple"
        4 -> "red"
        5 -> "black"
        else -> null
    }

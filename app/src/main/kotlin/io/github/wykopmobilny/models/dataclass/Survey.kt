package io.github.wykopmobilny.models.dataclass

data class Survey(
    val question: String,
    val answers: List<Answer>,
    val userAnswer: Int?,
    // Klucz ankiety z API - przy edycji wpisu trzeba go odeslac, inaczej ankieta znika.
    val key: String? = null,
)

package io.github.wykopmobilny.ui.fragments.entries

import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.models.dataclass.Entry
import io.reactivex.Single
import javax.inject.Inject

class EntriesInteractor
    @Inject
    constructor(
        val entriesApi: EntriesApi,
    ) {
        fun voteEntry(entry: Entry): Single<Entry> =
            entriesApi
                .voteEntry(entry.id)
                .map {
                    it.voteCount?.let { entry.voteCount = it } ?: entry.voteCount++
                    entry.isVoted = true
                    entry
                }

        fun unvoteEntry(entry: Entry): Single<Entry> =
            entriesApi
                .unvoteEntry(entry.id)
                .map {
                    it.voteCount?.let { entry.voteCount = it } ?: entry.voteCount--
                    entry.isVoted = false
                    entry
                }

        fun markFavorite(entry: Entry): Single<Entry> =
            entriesApi
                .markFavorite(entry.id, entry.isFavorite)
                .map {
                    entry.isFavorite = it
                    entry
                }

        // Toggle obserwowania dyskusji - kierunek wg aktualnego stanu, flaga aktualizowana
        // po sukcesie (optymistycznie w obiekcie backing, jak pozostale akcje).
        fun observeDiscussion(entry: Entry): Single<Entry> {
            val call =
                if (entry.isObservedDiscussion) {
                    entriesApi.unobserveDiscussion(entry.id)
                } else {
                    entriesApi.observeDiscussion(entry.id)
                }
            return call.map {
                entry.isObservedDiscussion = !entry.isObservedDiscussion
                entry
            }
        }

        fun deleteEntry(entry: Entry): Single<Entry> =
            entriesApi
                .deleteEntry(entry.id)
                .map {
                    entry.embed = null
                    entry.survey = null
                    entry.body = "[Wpis usunięty]"
                    entry
                }

        fun voteSurvey(
            entry: Entry,
            index: Int,
        ): Single<Entry> =
            entriesApi
                .voteSurvey(entry.id, index)
                .map { voted ->
                    // Glos (201) nie zwraca body. NIE podmieniamy ankiety na pusta (crashowalo
                    // render: getChildAt na pustej liscie odpowiedzi) - zachowujemy istniejaca,
                    // optymistycznie dodajemy glos wybranej odpowiedzi i przeliczamy procenty.
                    entry.survey =
                        entry.survey?.let { current ->
                            val updatedAnswers =
                                current.answers.mapIndexed { i, answer ->
                                    if (i == index - 1) answer.copy(count = answer.count + 1) else answer
                                }
                            val total = updatedAnswers.sumOf { it.count }
                            current.copy(
                                answers =
                                    updatedAnswers.map { answer ->
                                        answer.copy(percentage = if (total > 0) answer.count * 100.0 / total else 0.0)
                                    },
                                userAnswer = index,
                            )
                        } ?: voted
                    entry
                }
    }

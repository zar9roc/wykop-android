package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.entries.SurveyAnswerResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.SurveyResponseV3
import io.github.wykopmobilny.models.dataclass.Answer
import io.github.wykopmobilny.models.dataclass.Survey
import io.github.wykopmobilny.models.mapper.Mapper

object SurveyMapperV3 : Mapper<SurveyResponseV3, Survey> {
    override fun map(value: SurveyResponseV3): Survey {
        // API nie zwraca procentow - liczymy z count/suma.
        // Top-level `voted` to flaga "czy glosowal" (1/0), NIE id odpowiedzi - uzycie go
        // jako pozycji pokazywalo kazdy glos jako pierwsza opcje. Zaznaczona odpowiedz
        // wskazuje per-answer `voted` (0/1); userAnswer = jej pozycja 1-based.
        val total = value.count ?: value.answers.sumOf { it.count }
        val votedIndex = value.answers.indexOfFirst { (it.voted ?: 0) > 0 }
        return Survey(
            question = value.question,
            answers = value.answers.map { mapAnswer(it, total) },
            userAnswer = votedIndex.takeIf { it >= 0 }?.plus(1),
        )
    }

    private fun mapAnswer(
        answer: SurveyAnswerResponseV3,
        total: Int,
    ): Answer =
        Answer(
            id = answer.id,
            answer = answer.text,
            count = answer.count,
            percentage = if (total > 0) answer.count * PERCENT / total else 0.0,
        )

    private const val PERCENT = 100.0
}

package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.entries.SurveyAnswerResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.SurveyResponseV3
import io.github.wykopmobilny.models.dataclass.Answer
import io.github.wykopmobilny.models.dataclass.Survey
import io.github.wykopmobilny.models.mapper.Mapper

object SurveyMapperV3 : Mapper<SurveyResponseV3, Survey> {
    override fun map(value: SurveyResponseV3): Survey {
        // API nie zwraca procentow - liczymy z count/suma. userAnswer to 1-based id
        // zaznaczonej odpowiedzi (top-level `voted`); 0 = brak glosu -> null.
        val total = value.count ?: value.answers.sumOf { it.count }
        return Survey(
            question = value.question,
            answers = value.answers.map { mapAnswer(it, total) },
            userAnswer = value.voted?.takeIf { it > 0 },
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

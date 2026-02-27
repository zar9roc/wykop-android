package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.entries.SurveyAnswerResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.SurveyResponseV3
import io.github.wykopmobilny.models.dataclass.Answer
import io.github.wykopmobilny.models.dataclass.Survey
import io.github.wykopmobilny.models.mapper.Mapper

object SurveyMapperV3 : Mapper<SurveyResponseV3, Survey> {
    override fun map(value: SurveyResponseV3) =
        Survey(
            value.question,
            value.answers.map { mapAnswer(it) },
            value.userAnswer,
        )

    private fun mapAnswer(answer: SurveyAnswerResponseV3): Answer =
        Answer(
            id = answer.id,
            answer = answer.answer,
            count = answer.count,
            percentage = answer.percentage.toFloat(),
        )
}

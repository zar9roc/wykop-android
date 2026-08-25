package io.github.wykopmobilny.ui.modules.input.entry.add

import io.github.wykopmobilny.ui.modules.input.BaseInputView

interface AddEntryActivityView : BaseInputView {
    fun openEntryActivity(id: Long)

    // Ankieta utworzona (POST /v3/entries/survey) - pokaz podglad zalacznika.
    fun onSurveyCreated(
        question: String,
        answers: List<String>,
    )
}

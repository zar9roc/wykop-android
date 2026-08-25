package io.github.wykopmobilny.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.BottomsheetSurveyAnswerBinding
import io.github.wykopmobilny.databinding.BottomsheetSurveyBinding
import io.github.wykopmobilny.databinding.DialogEdittextBinding
import io.github.wykopmobilny.databinding.DialogInsertLinkBinding
import io.github.wykopmobilny.databinding.DialogNoteBinding
import io.github.wykopmobilny.utils.layoutInflater

typealias FormatDialogCallback = (String) -> Unit
typealias AddRelatedDialogCallback = (String, String) -> Unit

// Popup edycji notatki o uzytkowniku. Czyste UI - zapis/usuniecie robi caller w onSave.
// Usuwanie: wyczysc pole (X w polu) i zapisz - pusta tresc = usuniecie notatki.
fun noteDialog(
    context: Context,
    initialContent: String?,
    onSave: (String) -> Unit,
): AlertDialog {
    val binding = DialogNoteBinding.inflate(context.layoutInflater)
    val hasNote = !initialContent.isNullOrBlank()
    binding.noteDialogTitle.setText(if (hasNote) R.string.note_edit_title else R.string.note_add_title)
    binding.noteEditText.setText(initialContent.orEmpty())
    val dialog =
        AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(true)
            .create()
    binding.noteCancel.setOnClickListener { dialog.dismiss() }
    binding.noteSave.setOnClickListener {
        onSave(binding.noteEditText.text.toString())
        dialog.dismiss()
    }
    return dialog
}

private const val SURVEY_MIN_ANSWERS = 2
private const val SURVEY_MAX_ANSWERS = 10

// Serwer odrzuca krotsze pytania (walidacja jak przy tresci wpisu).
private const val SURVEY_MIN_QUESTION_LENGTH = 5

// Kreator ankiety wpisu w stylu bottomsheeta dodawania obrazka (naglowek "Nowa ankieta",
// wiersze-opcje pytania i odpowiedzi bedace inputami - tap fokusuje i podnosi klawiature -
// opcja "Dodaj odpowiedz" oraz "Zapisz"). Czyste UI - POST /v3/entries/survey robi caller
// w onSubmit. Prefill (existing*) sluzy edycji zcacheowanej ankiety.
fun surveyDialog(
    context: Context,
    existingQuestion: String?,
    existingAnswers: List<String>?,
    onSubmit: (question: String, answers: List<String>) -> Unit,
): BottomSheetDialog {
    val binding = BottomsheetSurveyBinding.inflate(context.layoutInflater)
    val dialog = BottomSheetDialog(context)
    dialog.setContentView(binding.root)

    // X (cofnij odpowiedz) widoczny od 3. odpowiedzi; "Dodaj odpowiedz" znika po osiagnieciu limitu.
    fun refreshRows() {
        binding.answersContainer.children.forEachIndexed { index, row ->
            BottomsheetSurveyAnswerBinding.bind(row).answerRemove.isVisible = index >= SURVEY_MIN_ANSWERS
        }
        binding.surveyAddAnswer.isVisible = binding.answersContainer.childCount < SURVEY_MAX_ANSWERS
    }

    fun addAnswerRow(text: String) {
        val row = BottomsheetSurveyAnswerBinding.inflate(context.layoutInflater, binding.answersContainer, false)
        row.answerInput.setText(text)
        row.answerRemove.setOnClickListener {
            binding.answersContainer.removeView(row.root)
            refreshRows()
        }
        binding.answersContainer.addView(row.root)
    }

    binding.surveyQuestion.setText(existingQuestion.orEmpty())
    val initial = (existingAnswers?.takeIf { it.isNotEmpty() } ?: listOf("", "")).take(SURVEY_MAX_ANSWERS)
    initial.forEach(::addAnswerRow)
    while (binding.answersContainer.childCount < SURVEY_MIN_ANSWERS) addAnswerRow("")
    refreshRows()

    binding.surveyAddAnswer.setOnClickListener {
        if (binding.answersContainer.childCount >= SURVEY_MAX_ANSWERS) return@setOnClickListener
        addAnswerRow("")
        refreshRows()
        val newRow = binding.answersContainer.getChildAt(binding.answersContainer.childCount - 1)
        val input = BottomsheetSurveyAnswerBinding.bind(newRow).answerInput
        input.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }
    binding.surveySave.setOnClickListener {
        val question = binding.surveyQuestion.text?.toString()?.trim().orEmpty()
        val answers =
            binding.answersContainer.children
                .map { BottomsheetSurveyAnswerBinding.bind(it).answerInput.text?.toString()?.trim().orEmpty() }
                .filter { it.isNotBlank() }
                .toList()
        if (question.length < SURVEY_MIN_QUESTION_LENGTH) {
            Toast.makeText(context, R.string.survey_min_question_length, Toast.LENGTH_SHORT).show()
        } else if (answers.size < SURVEY_MIN_ANSWERS) {
            Toast.makeText(context, R.string.survey_min_answers, Toast.LENGTH_SHORT).show()
        } else {
            onSubmit(question, answers)
            dialog.dismiss()
        }
    }
    return dialog
}

fun editTextFormatDialog(
    titleId: Int,
    context: Context,
    callback: FormatDialogCallback,
): AlertDialog {
    val editTextLayout = getEditTextView(context)
    AlertDialog.Builder(context).run {
        setTitle(titleId)
        setView(editTextLayout.root)
        setPositiveButton(android.R.string.ok) { _, _ -> callback.invoke(editTextLayout.editText.text.toString()) }
        return create()
    }
}

fun lennyfaceDialog(
    context: Context,
    callback: FormatDialogCallback,
): AlertDialog {
    AlertDialog.Builder(context).run {
        setTitle(R.string.insert_emoticon)
        val lennyArray =
            context.resources
                .getStringArray(R.array.lenny_face_array)
                .map { it.replace(" ", "\u00A0") }
                .toTypedArray()
        setItems(lennyArray) { _, pos -> callback.invoke(lennyArray[pos]) }
        return create()
    }
}

fun confirmationDialog(
    context: Context,
    callback: () -> Unit,
): AlertDialog {
    AlertDialog.Builder(context).run {
        setMessage(context.resources.getString(R.string.confirmation))
        setPositiveButton(android.R.string.ok) { _, _ -> callback.invoke() }
        setNegativeButton(android.R.string.cancel, null)
        setCancelable(true)
        return create()
    }
}

fun addRelatedDialog(
    context: Context,
    callback: AddRelatedDialogCallback,
): AlertDialog {
    val editTextLayout = DialogInsertLinkBinding.inflate(context.layoutInflater)
    AlertDialog.Builder(context).run {
        setTitle("Dodaj powiązane")
        setView(editTextLayout.root)
        setPositiveButton(android.R.string.ok) { _, _ ->
            callback(editTextLayout.link.text.toString(), editTextLayout.description.text.toString())
        }
        return create()
    }
}

private fun getEditTextView(context: Context) = DialogEdittextBinding.inflate(context.layoutInflater)

package io.github.wykopmobilny.ui.modules.input.entry.add

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.ui.dialogs.surveyDialog
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.ui.modules.input.BaseInputActivity
import javax.inject.Inject

class AddEntryActivity :
    BaseInputActivity<AddEntryPresenter>(),
    AddEntryActivityView {
    companion object {
        fun createIntent(
            context: Activity,
            receiver: String?,
            textBody: String? = null,
        ) = Intent(context, AddEntryActivity::class.java).apply {
            putExtra(EXTRA_BODY, textBody)
            putExtra(EXTRA_RECEIVER, receiver)
        }
    }

    @Inject
    override lateinit var suggestionApi: SuggestApi

    @Inject
    override lateinit var presenter: AddEntryPresenter

    @Inject
    lateinit var navigator: NewNavigator

    // Zcache'owana tresc ankiety - do prefillu przy ponownym otwarciu (edycji).
    private var surveyQuestion: String? = null
    private var surveyAnswers: List<String> = emptyList()

    override fun openEntryActivity(id: Long) {
        navigator.openEntryDetailsActivity(id, false)
        finish()
    }

    // Kafelek ankiety w toolbarze: dialog z pytaniem i odpowiedziami (prefill przy edycji).
    // Zapis => POST /v3/entries/survey (przez presenter), nowe survey_id za kazdym razem.
    override fun onSurveyClicked() {
        surveyDialog(
            context = this,
            existingQuestion = surveyQuestion,
            existingAnswers = surveyAnswers.ifEmpty { null },
        ) { question, answers ->
            presenter.createSurvey(question, answers)
        }.show()
    }

    override fun onSurveyCreated(
        question: String,
        answers: List<String>,
    ) {
        surveyQuestion = question
        surveyAnswers = answers
        binding.surveyPreviewText.text = getString(R.string.survey_attached, question)
        binding.surveyPreview.isVisible = true
    }

    // Zalaczona ankieta tez liczy sie jako niezapisana zmiana - wyjscie wymaga potwierdzenia.
    override fun hasUnsavedContent(): Boolean = super.hasUnsavedContent() || presenter.pendingSurveyId != null

    private fun clearSurvey() {
        presenter.pendingSurveyId = null
        surveyQuestion = null
        surveyAnswers = emptyList()
        binding.surveyPreview.isVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.subscribe(this)
        setupSuggestions()
        supportActionBar?.setTitle(R.string.add_new_entry)
        binding.markupToolbar.surveyEnabled = true
        // Klik w wiersz podgladu (poza X) wraca do edycji ankiety - X ma wlasny listener,
        // ktory konsumuje dotkniecie, wiec nie odpala edycji.
        binding.surveyPreview.setOnClickListener { onSurveyClicked() }
        binding.surveyPreviewRemove.setOnClickListener { clearSurvey() }

        if (intent.action == Intent.ACTION_SEND && intent.type != null) {
            if (intent.type == "text/plain") {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                text?.let {
                    textBody = text
                }
            } else if (intent.type?.startsWith("image/") == true) {
                val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                imageUri?.let {
                    binding.markupToolbar.photo = imageUri
                    Napier.d("Image uri $imageUri")
                }
            }
        }
    }

    override fun onDestroy() {
        presenter.unsubscribe()
        super.onDestroy()
    }
}

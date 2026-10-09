package io.github.wykopmobilny.ui.modules.input.entry.edit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.ui.widgets.MAX_MICROBLOG_PHOTOS
import io.github.wykopmobilny.ui.modules.input.BaseInputActivity
import javax.inject.Inject

class EditEntryActivity :
    BaseInputActivity<EditEntryPresenter>(),
    EditEntryView {
    companion object {
        const val EXTRA_ENTRY_ID = "ENTRY_ID"
        const val EXTRA_SURVEY_KEY = "SURVEY_KEY"

        fun createIntent(
            context: Context,
            body: String,
            entryId: Long,
            attachments: List<Embed>,
            surveyKey: String?,
        ) = Intent(context, EditEntryActivity::class.java).apply {
            putExtra(EXTRA_SURVEY_KEY, surveyKey)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_ENTRY_ID, entryId)
            putParcelableArrayListExtra(EXTRA_ATTACHMENTS, ArrayList(attachments))
        }
    }

    @Inject
    override lateinit var presenter: EditEntryPresenter

    @Inject
    override lateinit var suggestionApi: SuggestApi

    override val entryId by lazy { intent.getLongExtra(EXTRA_ENTRY_ID, 0) }
    override val surveyKey: String? by lazy { intent.getStringExtra(EXTRA_SURVEY_KEY) }
    override val maxPhotos = MAX_MICROBLOG_PHOTOS

    private val attachments: List<Embed> by lazy {
        @Suppress("DEPRECATION")
        intent.getParcelableArrayListExtra<Embed>(EXTRA_ATTACHMENTS).orEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.subscribe(this)
        setupSuggestions()
        supportActionBar?.setTitle(R.string.edit_entry)
        prefillAttachments(attachments)
    }

    override fun onDestroy() {
        presenter.unsubscribe()
        super.onDestroy()
    }

    override fun exitActivity() {
        val data = Intent()
        data.putExtra("entryId", entryId)
        data.putExtra("entryBody", textBody)
        setResult(Activity.RESULT_OK, data)
        finish()
    }
}

package io.github.wykopmobilny.ui.modules.input

import android.app.Activity
import androidx.activity.addCallback
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.isVisible
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.base.BaseActivity
import io.github.wykopmobilny.databinding.ActivityWriteCommentBinding
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.ui.dialogs.exitConfirmationDialog
import io.github.wykopmobilny.ui.suggestions.HashTagsSuggestionsAdapter
import io.github.wykopmobilny.ui.suggestions.UsersSuggestionsAdapter
import io.github.wykopmobilny.ui.suggestions.WykopSuggestionsTokenizer
import io.github.wykopmobilny.ui.widgets.ZERO_WIDTH_SPACE
import io.github.wykopmobilny.ui.widgets.markdowntoolbar.MarkdownToolbarListener
import io.github.wykopmobilny.utils.textview.stripWykopFormatting
import io.github.wykopmobilny.utils.viewBinding

abstract class BaseInputActivity<T : BaseInputPresenter> :
    BaseActivity(),
    BaseInputView,
    MarkdownToolbarListener {
    companion object {
        const val EXTRA_RECEIVER = "EXTRA_RECEIVER"
        const val EXTRA_BODY = "EXTRA_BODY"
        const val EXTRA_EMBED = "EXTRA_EMBED"
        const val EXTRA_ATTACHMENTS = "EXTRA_ATTACHMENTS"
        const val REQUEST_CODE = 106
        const val EDIT_ENTRY_COMMENT = 107
        const val EDIT_ENTRY = 108
        const val EDIT_LINK_COMMENT = 109
        const val USER_ACTION_INSERT_PHOTO = 142
        const val USER_ACTION_INSERT_PHOTO_CAMERA = 143
    }

    abstract var suggestionApi: SuggestApi

    /** Limit zdjec: 4 dla wpisow i komentarzy mikrobloga, 1 dla komentarzy znalezisk. */
    protected open val maxPhotos: Int = 1
    abstract var presenter: T

    private val usersSuggestionAdapter by lazy { UsersSuggestionsAdapter(this, suggestionApi) }
    private val hashTagsSuggestionAdapter by lazy { HashTagsSuggestionsAdapter(this, suggestionApi) }

    lateinit var contentUri: Uri

    override var textBody: String
        get() =
            if (
                (binding.markupToolbar.hasPhotos || binding.markupToolbar.embedUrl != null) &&
                binding.body.text.isEmpty()
            ) {
                ZERO_WIDTH_SPACE
            } else {
                binding.body.text.toString()
            }
        set(value) {
            binding.body.setText(value, TextView.BufferType.EDITABLE)
        }

    override var selectionStart: Int
        get() = binding.body.selectionStart
        set(value) {
            binding.body.setSelection(value)
        }

    override var selectionEnd: Int
        get() = binding.body.selectionEnd
        set(value) {
            binding.body.setSelection(value)
        }

    fun setupSuggestions() {
        binding.body.setTokenizer(
            WykopSuggestionsTokenizer(
                {
                    if (binding.body.adapter !is UsersSuggestionsAdapter) {
                        binding.body.setAdapter(usersSuggestionAdapter)
                    }
                },
                {
                    if (binding.body.adapter !is HashTagsSuggestionsAdapter) {
                        binding.body.setAdapter(hashTagsSuggestionAdapter)
                    }
                },
            ),
        )
        binding.body.threshold = 3
    }

    protected val binding by viewBinding(ActivityWriteCommentBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var initialSelection: Int? = null
        intent.apply {
            getStringExtra(EXTRA_RECEIVER)?.apply {
                textBody += "$this: "
                initialSelection = this.length + 2
            }

            getStringExtra(EXTRA_BODY)?.apply {
                // @TODO Replace it with some regex or parser, its way too hacky now
                textBody += stripWykopFormatting()
                initialSelection =
                    if (!startsWith("#")) {
                        textBody.length
                    } else {
                        // Tresc od tagu (wpis z widoku tagu): tag linijke nizej,
                        // kursor na poczatku (0,0) - user pisze NAD tagiem.
                        textBody = "\n$textBody"
                        0
                    }
            }
        }

        binding.markupToolbar.markdownListener = this
        binding.markupToolbar.floatingImageView = binding.floatingImageView
        binding.markupToolbar.maxPhotos = maxPhotos

        // targetSdk 36 + enableOnBackInvokedCallback: onBackPressed() nie jest wołany,
        // potwierdzenie wyjścia przy niezapisanej treści musi iść przez dispatcher.
        onBackPressedDispatcher.addCallback(this) {
            if (!hasUnsavedContent()) {
                exitActivity()
            } else {
                exitConfirmationDialog(this@BaseInputActivity) { exitActivity() }?.show()
            }
        }

        // show focus
        binding.body.requestFocus()
        // Kursor ustawiany PO requestFocus: przy braku layoutu (jestesmy w onCreate)
        // ArrowKeyMovementMethod.onTakeFocus przeskakuje na koniec tekstu, kasujac
        // wczesniejsze setSelection (kursor mial byc np. na 0,0 nad tagiem).
        initialSelection?.let { selectionStart = it }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.body, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun setSelection(
        start: Int,
        end: Int,
    ) = binding.body.setSelection(start, end)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_comment, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.send -> {
                presenter.send(
                    binding.markupToolbar.getPhotoSources(),
                    binding.markupToolbar.containsAdultContent,
                    binding.markupToolbar.embedUrl,
                )
            }

            android.R.id.home -> {
                onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                // Gallery chooser's callback
                // Przy wielokrotnym wyborze adresy sa w clipData, przy pojedynczym w data.
                USER_ACTION_INSERT_PHOTO -> {
                    data
                        ?.selectedUris()
                        .orEmpty()
                        .take(binding.markupToolbar.remainingPhotos.coerceAtLeast(1))
                        .forEach(binding.markupToolbar::addPhoto)
                }

                USER_ACTION_INSERT_PHOTO_CAMERA -> {
                    binding.markupToolbar.addPhoto(contentUri)
                }
            }
        }
    }

    // Shows "sending entry" progress in notification
    override var showProgressBar: Boolean
        get() = binding.progressBar.isVisible
        set(value) {
            binding.progressBar.isVisible = value
            binding.contentView.isVisible = !value
            binding.markupToolbar.isVisible = !value
        }

    // Guard wyjscia: czy sa niezapisane zmiany wymagajace potwierdzenia. Bazowo tresc/zdjecie
    // (MarkdownToolbar); podklasy dokladaja swoje zalaczniki (np. AddEntry - ankieta).
    protected open fun hasUnsavedContent(): Boolean = binding.markupToolbar.hasUserEditedContent()

    /**
     * Edycja: istniejace zalaczniki trafiaja do paska jako adresy (przy wysylce
     * serwer dostaje je z powrotem przez /media/photos), embed do slotu linku.
     */
    protected fun prefillAttachments(attachments: List<Embed>) {
        binding.markupToolbar.containsAdultContent = attachments.any { it.plus18 }
        attachments.forEach { attachment ->
            if (attachment.type == "image") {
                binding.markupToolbar.addPhotoUrl(attachment.url)
            } else {
                binding.markupToolbar.embedUrl = attachment.url
            }
        }
    }

    override fun exitActivity() {
        setResult(Activity.RESULT_OK)
        finish()
    }


    override fun openGalleryImageChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, binding.markupToolbar.remainingPhotos > 1)
        startActivityForResult(
            Intent.createChooser(
                intent,
                getString(R.string.insert_photo_galery),
            ),
            USER_ACTION_INSERT_PHOTO,
        )
    }

    override fun openCamera(uri: Uri) {
        contentUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, USER_ACTION_INSERT_PHOTO_CAMERA)
    }
}

private fun Intent.selectedUris(): List<Uri> {
    val clip = clipData ?: return listOfNotNull(data)
    return (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
}

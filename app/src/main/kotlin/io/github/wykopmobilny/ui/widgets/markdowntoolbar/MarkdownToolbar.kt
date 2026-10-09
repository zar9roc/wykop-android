package io.github.wykopmobilny.ui.widgets.markdowntoolbar

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.PhotoSource
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.looksLikeDirectImageUrl
import io.github.wykopmobilny.databinding.ImagechooserBottomsheetBinding
import io.github.wykopmobilny.databinding.MarkdownToolbarBinding
import io.github.wykopmobilny.ui.dialogs.FormatDialogCallback
import io.github.wykopmobilny.ui.dialogs.editTextFormatDialog
import io.github.wykopmobilny.ui.widgets.FloatingImageView
import io.github.wykopmobilny.utils.CameraUtils
import io.github.wykopmobilny.utils.getActivityContext
import io.github.wykopmobilny.utils.layoutInflater

class MarkdownToolbar(
    context: Context,
    attrs: AttributeSet?,
) : LinearLayout(context, attrs) {
    // Link do serwisu medialnego (YouTube itp.) - osobny slot obok zdjec,
    // wysylany jako pole "embed" (klucz z POST /media/embed).
    var embedUrl: String?
        get() = floatingImageView?.embedUrl
        set(value) {
            if (value != null) {
                remoteImageInserted()
                floatingImageView?.loadEmbedUrl(value)
            } else {
                floatingImageView?.clearEmbed()
            }
        }

    var markdownListener: MarkdownToolbarListener? = null
    var remoteImageInserted: () -> Unit = {}
    var containsAdultContent = false
    var floatingImageView: FloatingImageView? = null
        set(value) {
            field = value
            value?.maxPhotos = maxPhotos
            value?.onAddPhotoClick = ::showUploadPhotoBottomsheet
        }

    /** Limit zdjec: 4 we wpisach i komentarzach mikrobloga, 1 w pozostalych miejscach. */
    var maxPhotos: Int = 1
        set(value) {
            field = value
            floatingImageView?.maxPhotos = value
        }

    val hasPhotos: Boolean
        get() = floatingImageView?.photos?.isNotEmpty() == true

    val remainingPhotos: Int
        get() = floatingImageView?.remainingPhotos ?: maxPhotos

    fun addPhoto(uri: Uri) {
        floatingImageView?.addPhoto(FloatingImageView.Photo.Local(uri))
    }

    fun addPhotoUrl(url: String) {
        remoteImageInserted()
        floatingImageView?.addPhoto(FloatingImageView.Photo.Remote(url))
    }

    fun clearAttachments() {
        floatingImageView?.removeImage()
    }

    // Kafelek ankiety domyslnie ukryty - wlacza go tylko ekran dodawania wpisu
    // (ankiety dotycza wpisow, nie komentarzy/PM).
    var surveyEnabled: Boolean
        get() = binding.insertSurvey.visibility == View.VISIBLE
        set(value) {
            binding.insertSurvey.visibility = if (value) View.VISIBLE else View.GONE
        }

    private val binding = MarkdownToolbarBinding.inflate(layoutInflater, this, true)
    private val markdownDialogs by lazy { MarkdownDialogs(context) }
    private val formatText: FormatDialogCallback = {
        markdownListener?.apply {
            val prefix = textBody.substring(0, selectionStart)
            textBody = prefix + it + textBody.substring(selectionStart, textBody.length)
            selectionStart = prefix.length + it.length
        }
    }

    init {
        binding.insertSurvey.setOnClickListener { markdownListener?.onSurveyClicked() }

        // Create callbacks
        markdownDialogs.apply {
            binding.formatBold.setOnClickListener { insertFormat("**", "**") }
            binding.formatQuote.setOnClickListener { insertFormat("\n>", "") }
            binding.formatItalic.setOnClickListener { insertFormat("_", "_") }
            binding.insertLink.setOnClickListener { insertFormat("[", "](www.wykop.pl)") }
            binding.insertCode.setOnClickListener { insertFormat("`", "`") }
            binding.insertSpoiler.setOnClickListener { insertFormat("\n!", "") }
            binding.insertEmoticon.setOnClickListener { showLennyfaceDialog(formatText) }
            // Zadna z opcji nie wymaga uprawnien: galeria = ACTION_GET_CONTENT,
            // aparat = FileProvider w katalogu aplikacji, URL = wpisanie adresu.
            // Stary gate na WRITE_EXTERNAL_STORAGE blokowal caly wybor zdjecia,
            // bo na Androidzie 11+ system zawsze odmawia tego uprawnienia.
            binding.insertPhoto.setOnClickListener {
                if (maxPhotos > 1 && remainingPhotos == 0) {
                    Toast.makeText(context, context.getString(R.string.attachment_photo_limit, maxPhotos), Toast.LENGTH_SHORT).show()
                } else {
                    showUploadPhotoBottomsheet()
                }
            }
        }
    }

    fun getPhotoSources(): List<PhotoSource> =
        floatingImageView?.photos.orEmpty().map { photo ->
            when (photo) {
                is FloatingImageView.Photo.Local -> PhotoSource.File(WykopImageFile(photo.uri, context))
                is FloatingImageView.Photo.Remote -> PhotoSource.Url(photo.url)
            }
        }

    fun hasUserEditedContent(): Boolean =
        (
            hasPhotos ||
                !floatingImageView?.embedUrl.isNullOrEmpty() ||
                (markdownListener != null && markdownListener?.textBody!!.isNotEmpty())
        )

    private fun insertFormat(
        prefix: String,
        suffix: String,
    ) {
        markdownListener?.apply {
            if (selectionEnd > selectionStart) {
                val bodyPrefix = textBody.substring(0, selectionStart)
                val bodySuffix = textBody.substring(selectionEnd, textBody.length)
                val selectedText = textBody.substring(selectionStart, selectionEnd)
                textBody = bodyPrefix + prefix + selectedText + suffix + bodySuffix
                setSelection(bodyPrefix.length + prefix.length, bodyPrefix.length + prefix.length + selectedText.length)
            } else {
                val bodyPrefix = textBody.substring(0, selectionStart)
                val bodySuffix = textBody.substring(selectionStart, textBody.length)
                val selectedText = "tekst"
                textBody = bodyPrefix + prefix + selectedText + suffix + bodySuffix
                setSelection(bodyPrefix.length + prefix.length, bodyPrefix.length + prefix.length + selectedText.length)
            }
        }
    }

    private fun showUploadPhotoBottomsheet() {
        val activityContext = getActivityContext()!!
        val dialog = BottomSheetDialog(activityContext)
        val bottomSheetView = ImagechooserBottomsheetBinding.inflate(activityContext.layoutInflater)
        dialog.setContentView(bottomSheetView.root)

        bottomSheetView.apply {
            insertGallery.setOnClickListener {
                markdownListener?.openGalleryImageChooser()
                dialog.dismiss()
            }

            insertCamera.setOnClickListener {
                val cameraUri = CameraUtils.createPictureUri(context)
                markdownListener?.openCamera(cameraUri!!)
                dialog.dismiss()
            }

            insertUrl.setOnClickListener {
                editTextFormatDialog(R.string.insert_photo_url, context) { insertImageFromUrl(it) }.show()
                dialog.dismiss()
            }

            markNsfwCheckbox.isChecked = containsAdultContent
            markNsfwCheckbox.setOnCheckedChangeListener { _, isChecked ->
                containsAdultContent = isChecked
            }

            markNsfw.setOnClickListener { markNsfwCheckbox.performClick() }
        }

        val mBehavior = BottomSheetBehavior.from(bottomSheetView.root.parent as View)
        dialog.setOnShowListener {
            mBehavior.peekHeight = bottomSheetView.root.height
        }
        dialog.show()
    }

    private fun insertImageFromUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        remoteImageInserted()
        // Bezposredni obrazek -> slot zdjecia (POST /media/photos); kazdy inny adres
        // (YouTube, streamable...) -> slot embedu (POST /media/embed przy wysylce).
        if (trimmed.looksLikeDirectImageUrl()) {
            floatingImageView?.addPhoto(FloatingImageView.Photo.Remote(trimmed))
        } else {
            floatingImageView?.loadEmbedUrl(trimmed)
        }
    }
}

package io.github.wykopmobilny.ui.widgets

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.FloatingImageViewLayoutBinding
import io.github.wykopmobilny.ui.modules.embedview.YouTubeUrlParser
import io.github.wykopmobilny.utils.layoutInflater
import io.github.wykopmobilny.utils.loadImage

/**
 * Podglad zalacznikow nad polem tekstu. Dwa niezalezne sloty (kazdy z wlasnym koszem):
 * zdjecie (plik z galerii/aparatu albo URL obrazka) oraz link medialny (YouTube itp.),
 * wysylany jako "embed" - moga byc zalaczone rownoczesnie.
 */
class FloatingImageView(
    context: Context,
    attrs: AttributeSet,
) : FrameLayout(context, attrs) {
    private val binding = FloatingImageViewLayoutBinding.inflate(layoutInflater, this)

    init {
        this.isVisible = false
        binding.deleteButton.setOnClickListener { clearPhoto() }
        binding.embedDeleteButton.setOnClickListener { clearEmbed() }
    }

    var photo: Uri? = null
    var photoUrl: String? = null

    /** Link do serwisu medialnego (YouTube itp.) - wysylany jako embed, nie zdjecie. */
    var embedUrl: String? = null

    fun clearPhoto() {
        photo = null
        photoUrl = null
        binding.imageCard.isVisible = false
        binding.deleteButton.isVisible = false
        refreshVisibility()
    }

    fun clearEmbed() {
        embedUrl = null
        binding.embedCard.isVisible = false
        binding.embedDeleteButton.isVisible = false
        refreshVisibility()
    }

    fun removeImage() {
        clearPhoto()
        clearEmbed()
    }

    fun loadPhotoUrl(photoUrl: String) {
        photo = null
        this.photoUrl = photoUrl
        binding.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.imageView.loadImage(photoUrl)
        showPhotoSlot()
    }

    /**
     * Podglad linku medialnego: miniatura YouTube gdy da sie ja wyliczyc lokalnie,
     * w innym razie monochromatyczna ikona linku (tint z motywu).
     */
    fun loadEmbedUrl(url: String) {
        embedUrl = url
        val videoId = YouTubeUrlParser.getVideoId(url)
        if (videoId != null) {
            binding.embedImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.embedImageView.loadImage("https://img.youtube.com/vi/$videoId/hqdefault.jpg")
        } else {
            binding.embedImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            binding.embedImageView.setImageResource(R.drawable.ic_link)
        }
        binding.embedCard.isVisible = true
        binding.embedDeleteButton.isVisible = true
        refreshVisibility()
    }

    fun setImage(photo: Uri?) {
        this.photo = photo
        photo?.let {
            binding.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load(photo).into(binding.imageView)
            showPhotoSlot()
        }
    }

    private fun showPhotoSlot() {
        binding.imageCard.isVisible = true
        binding.deleteButton.isVisible = true
        refreshVisibility()
    }

    private fun refreshVisibility() {
        isVisible = binding.imageCard.isVisible || binding.embedCard.isVisible
    }
}

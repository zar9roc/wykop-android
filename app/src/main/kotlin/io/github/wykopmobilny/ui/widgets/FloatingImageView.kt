package io.github.wykopmobilny.ui.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.FloatingImageViewLayoutBinding
import io.github.wykopmobilny.ui.modules.embedview.YouTubeUrlParser
import io.github.wykopmobilny.utils.layoutInflater
import kotlin.math.roundToInt

/** Limit zdjec we wpisie i komentarzu mikrobloga (API: maxItems 4). */
const val MAX_MICROBLOG_PHOTOS = 4

/**
 * Pasek zalacznikow nad polem tekstu: do [maxPhotos] zdjec (plik z galerii/aparatu
 * albo URL obrazka) oraz link medialny (YouTube itp.) wysylany jako "embed".
 * Kazdy zalacznik ma wlasne "x"; kolejnosc zdjec = kolejnosc dodania.
 */
class FloatingImageView(
    context: Context,
    attrs: AttributeSet,
) : FrameLayout(context, attrs) {
    sealed interface Photo {
        data class Local(
            val uri: Uri,
        ) : Photo

        data class Remote(
            val url: String,
        ) : Photo
    }

    private val binding = FloatingImageViewLayoutBinding.inflate(layoutInflater, this)
    private val mutablePhotos = mutableListOf<Photo>()

    val photos: List<Photo>
        get() = mutablePhotos

    /** 4 dla wpisow i komentarzy mikrobloga, 1 dla komentarzy znalezisk i PW. */
    var maxPhotos: Int = 1
        set(value) {
            field = value
            render()
        }

    val remainingPhotos: Int
        get() = (maxPhotos - mutablePhotos.size).coerceAtLeast(0)

    /** Link do serwisu medialnego (YouTube itp.) - wysylany jako embed, nie zdjecie. */
    var embedUrl: String? = null
        private set

    /** Kafel "+" - otwiera ten sam wybor zrodla co przycisk zdjecia na pasku formatowania. */
    var onAddPhotoClick: () -> Unit = {}

    /** Zmiana zestawu zalacznikow (np. zeby odswiezyc stan przycisku wysylki). */
    var onAttachmentsChanged: () -> Unit = {}

    init {
        this.isVisible = false
    }

    /** Przy limicie 1 nowe zdjecie zastepuje poprzednie (jak dotad); przy pelnej galerii jest pomijane. */
    fun addPhoto(photo: Photo) {
        if (maxPhotos <= 1) {
            mutablePhotos.clear()
        } else if (remainingPhotos == 0) {
            return
        }
        mutablePhotos += photo
        render()
    }

    fun clearPhotos() {
        mutablePhotos.clear()
        render()
    }

    fun loadEmbedUrl(url: String) {
        embedUrl = url
        render()
    }

    fun clearEmbed() {
        embedUrl = null
        render()
    }

    fun removeImage() {
        mutablePhotos.clear()
        embedUrl = null
        render()
    }

    private fun render() {
        val row = binding.attachmentRow
        row.removeAllViews()
        mutablePhotos.forEachIndexed { index, photo ->
            val tile =
                attachmentTile(
                    removeDescription = context.getString(R.string.attachment_remove_photo, index + 1),
                    onRemove = {
                        mutablePhotos.removeAt(index)
                        render()
                    },
                ) { image ->
                    image.scaleType = ImageView.ScaleType.CENTER_CROP
                    when (photo) {
                        is Photo.Local -> Glide.with(context).load(photo.uri).into(image)
                        is Photo.Remote -> Glide.with(context).load(photo.url).into(image)
                    }
                }
            row.addView(tile)
        }
        if (maxPhotos > 1 && mutablePhotos.isNotEmpty() && remainingPhotos > 0) {
            row.addView(addTile())
        }
        embedUrl?.let { url ->
            val tile =
                attachmentTile(
                    removeDescription = context.getString(R.string.attachment_remove_embed),
                    onRemove = ::clearEmbed,
                ) { image -> bindEmbedPreview(image, url) }
            row.addView(tile)
        }
        binding.photoCounter.isVisible = maxPhotos > 1 && mutablePhotos.isNotEmpty()
        binding.photoCounter.text = context.getString(R.string.attachment_counter, mutablePhotos.size, maxPhotos)
        isVisible = mutablePhotos.isNotEmpty() || embedUrl != null
        onAttachmentsChanged()
    }

    /**
     * Podglad linku medialnego: miniatura YouTube gdy da sie ja wyliczyc lokalnie,
     * w innym razie monochromatyczna ikona linku (tint z motywu).
     */
    private fun bindEmbedPreview(
        image: ImageView,
        url: String,
    ) {
        val videoId = YouTubeUrlParser.getVideoId(url)
        if (videoId != null) {
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(context).load("https://img.youtube.com/vi/$videoId/hqdefault.jpg").into(image)
        } else {
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            image.setImageResource(R.drawable.ic_link)
        }
    }

    private fun attachmentTile(
        removeDescription: String,
        onRemove: () -> Unit,
        bindImage: (ImageView) -> Unit,
    ): View {
        val size = dp(TILE_DP)
        val tile = FrameLayout(context)
        tile.layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(TILE_GAP_DP) }
        val image =
            // Zaokraglenie przez obrys widoku - ShapeableImageView szukal w motywie
            // atrybutow Material, ktorych motywy aplikacji nie maja (ostrzezenia w logu).
            AppCompatImageView(context).apply {
                outlineProvider = roundedOutline
                clipToOutline = true
                setBackgroundResource(R.drawable.bg_attachment_add)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        tile.addView(image, LayoutParams(size, size))
        bindImage(image)
        val remove =
            ImageView(context).apply {
                setImageResource(R.drawable.ic_close)
                imageTintList = ColorStateList.valueOf(Color.WHITE)
                setBackgroundResource(R.drawable.bg_attachment_remove)
                val padding = dp(REMOVE_PADDING_DP)
                setPadding(padding, padding, padding, padding)
                contentDescription = removeDescription
                setOnClickListener { onRemove() }
            }
        tile.addView(remove, LayoutParams(dp(REMOVE_DP), dp(REMOVE_DP), Gravity.TOP or Gravity.END))
        remove.updateLayoutParams<LayoutParams> { setMargins(0, dp(2), dp(2), 0) }
        return tile
    }

    private val roundedOutline =
        object : ViewOutlineProvider() {
            override fun getOutline(
                view: View,
                outline: Outline,
            ) = outline.setRoundRect(0, 0, view.width, view.height, dp(CORNER_DP).toFloat())
        }

    private fun addTile(): View {
        val size = dp(TILE_DP)
        return ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(TILE_GAP_DP) }
            setBackgroundResource(R.drawable.bg_attachment_add)
            setImageResource(R.drawable.ic_add)
            imageTintList = ColorStateList.valueOf(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorControlNormal))
            scaleType = ImageView.ScaleType.CENTER
            contentDescription = context.getString(R.string.attachment_add_photo)
            setOnClickListener { onAddPhotoClick() }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val TILE_DP = 64
        private const val TILE_GAP_DP = 8
        private const val CORNER_DP = 3
        private const val REMOVE_DP = 24
        private const val REMOVE_PADDING_DP = 4
    }
}

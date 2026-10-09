package io.github.wykopmobilny.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.github.wykopmobilny.R
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.models.dataclass.Embed
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Galeria do 4 zdjec wpisu/komentarza mikrobloga.
 *
 * Mozaika o stalej proporcji 4:3 (wysokosc znana przed zaladowaniem obrazkow,
 * wiec lista nie skacze przy scrollu): 1 kafel, 2 obok siebie, 3 = duzy z lewej
 * i dwa z prawej, 4 = siatka 2x2. Kafle przyciete (centerCrop) - calosc widac
 * w przegladarce. Przy "pomniejszonych obrazkach" albo waskim miejscu (gleboko
 * wciety komentarz watku) - pasek kwadratowych miniatur.
 *
 * Flaga 18+/nsfw dotyczy calej tresci, nie pojedynczego zdjecia, wiec zaslona
 * jest jedna na cala galerie i odslania wszystko naraz.
 */
class PhotoGridView(
    context: Context,
    attrs: AttributeSet?,
) : ViewGroup(context, attrs) {
    private val tiles = List(MAX_PHOTOS) { createTile() }
    private val gifChips = List(MAX_PHOTOS) { createChip(context.getString(R.string.gallery_gif_chip)) }
    private val cover = createTile().apply { isVisible = false }
    private val coverChip = createChip("").apply { isVisible = false }

    private var photos: List<Embed> = emptyList()
    private var hidden = false

    /** Dotkniecie kafla (indeks zdjecia) przy odslonietej galerii. */
    var onPhotoClick: (Int) -> Unit = {}

    /** Dotkniecie zaslony 18+/nsfw. */
    var onRevealClick: () -> Unit = {}

    private val settingsPreferencesApi by lazy { (context.applicationContext as WykopApp).settingsPreferencesApi.get() }

    init {
        tiles.forEachIndexed { index, tile ->
            addView(tile)
            tile.setOnClickListener { onPhotoClick(index) }
        }
        gifChips.forEach(::addView)
        addView(cover)
        addView(coverChip)
        cover.setOnClickListener { onRevealClick() }
    }

    fun setPhotos(
        photos: List<Embed>,
        hidden: Boolean,
        placeholderUrl: String,
    ) {
        this.photos = photos.take(MAX_PHOTOS)
        this.hidden = hidden
        val count = this.photos.size
        tiles.forEachIndexed { index, tile ->
            Glide.with(context).clear(tile)
            tile.setImageDrawable(null)
            val photo = this.photos.getOrNull(index)
            tile.isVisible = photo != null
            gifChips[index].isVisible = photo?.isAnimated == true && !hidden
            if (photo == null) return@forEachIndexed
            tile.contentDescription =
                context.getString(
                    if (photo.isAnimated) R.string.gallery_photo_gif_description else R.string.gallery_photo_description,
                    index + 1,
                    count,
                )
            // Pod zaslona kafle zostaja puste - nie sciagamy tresci, ktorej uzytkownik nie chce widziec.
            if (!hidden) loadTile(tile, photo.preview)
        }
        Glide.with(context).clear(cover)
        cover.isVisible = hidden
        coverChip.isVisible = hidden
        if (hidden) {
            loadTile(cover, placeholderUrl)
            coverChip.text = resources.getQuantityString(R.plurals.gallery_photo_count, count, count)
            cover.contentDescription = context.getString(R.string.gallery_hidden_description, count)
        }
        requestLayout()
    }

    // Statyczna miniatura (asBitmap) takze dla GIF-ow - cztery animacje w jednym
    // wierszu to za duzo pamieci; animacje gra przegladarka.
    private fun loadTile(
        tile: ImageView,
        url: String,
    ) {
        Glide
            .with(context)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(tile)
    }

    // Ustalane w onMeasure, uzywane w onLayout.
    private var stripMode = false

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width =
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                resources.displayMetrics.widthPixels
            } else {
                MeasureSpec.getSize(widthMeasureSpec)
            }
        stripMode = settingsPreferencesApi.showMinifiedImages || width < dp(MIN_GRID_WIDTH_DP)
        val height =
            if (stripMode) {
                dp(STRIP_TILE_DP)
            } else {
                val proportion =
                    (settingsPreferencesApi.cutImageProportion ?: WykopImageView.DEFAULT_CUT_IMAGE_PROPORTION).toFloat() / 100
                val maxHeight = (resources.displayMetrics.heightPixels * proportion).roundToInt()
                min(width * 3 / 4, maxHeight)
            }
        setMeasuredDimension(width, height)
        // Dzieci dostaja dokladne wymiary dopiero w onLayout (layoutExactly).
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val width = r - l
        val height = b - t
        val rects = if (stripMode) stripRects() else gridRects(width, height)
        tiles.forEachIndexed { index, tile ->
            val rect = rects.getOrNull(index)
            if (rect == null) {
                tile.layout(0, 0, 0, 0)
                gifChips[index].layout(0, 0, 0, 0)
            } else {
                layoutExactly(tile, rect)
                val chip = gifChips[index]
                chip.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
                val margin = dp(CHIP_MARGIN_DP)
                chip.layout(
                    rect.left + margin,
                    rect.bottom - margin - chip.measuredHeight,
                    rect.left + margin + chip.measuredWidth,
                    rect.bottom - margin,
                )
            }
        }
        // Zaslona przykrywa obszar wszystkich kafli.
        val union = Rect(0, 0, rects.maxOfOrNull { it.right } ?: 0, height)
        layoutExactly(cover, union)
        coverChip.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        val margin = dp(CHIP_MARGIN_DP)
        coverChip.layout(
            union.right - margin - coverChip.measuredWidth,
            union.bottom - margin - coverChip.measuredHeight,
            union.right - margin,
            union.bottom - margin,
        )
    }

    private fun stripRects(): List<Rect> {
        val size = dp(STRIP_TILE_DP)
        val gap = dp(STRIP_GAP_DP)
        return photos.indices.map { i -> Rect(i * (size + gap), 0, i * (size + gap) + size, size) }
    }

    private fun gridRects(
        width: Int,
        height: Int,
    ): List<Rect> {
        val gap = dp(GRID_GAP_DP)
        val halfW = (width - gap) / 2
        val halfH = (height - gap) / 2
        val right = halfW + gap
        val bottom = halfH + gap
        return when (photos.size) {
            0 -> emptyList()
            1 -> listOf(Rect(0, 0, width, height))
            2 -> listOf(Rect(0, 0, halfW, height), Rect(right, 0, width, height))
            3 -> listOf(Rect(0, 0, halfW, height), Rect(right, 0, width, halfH), Rect(right, bottom, width, height))
            else ->
                listOf(
                    Rect(0, 0, halfW, halfH),
                    Rect(right, 0, width, halfH),
                    Rect(0, bottom, halfW, height),
                    Rect(right, bottom, width, height),
                )
        }
    }

    private fun layoutExactly(
        view: View,
        rect: Rect,
    ) {
        view.measure(
            MeasureSpec.makeMeasureSpec(rect.width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(rect.height, MeasureSpec.EXACTLY),
        )
        view.layout(rect.left, rect.top, rect.right, rect.bottom)
    }

    private fun createTile() =
        AppCompatImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(themeColor(R.attr.lineColor))
            isClickable = true
            isFocusable = true
        }

    private fun createChip(text: String) =
        TextView(context).apply {
            this.text = text
            setTextColor(android.graphics.Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, CHIP_TEXT_SP)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_media_chip)
            val horizontal = dp(CHIP_PADDING_H_DP)
            val vertical = dp(CHIP_PADDING_V_DP)
            setPadding(horizontal, vertical, horizontal, vertical)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

    private fun themeColor(attr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(attr, value, true)
        return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    private data class Rect(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val width get() = right - left
        val height get() = bottom - top
    }

    companion object {
        const val MAX_PHOTOS = 4
        private const val MIN_GRID_WIDTH_DP = 260
        private const val STRIP_TILE_DP = 64
        private const val STRIP_GAP_DP = 4
        private const val GRID_GAP_DP = 2
        private const val CHIP_MARGIN_DP = 6
        private const val CHIP_PADDING_H_DP = 6
        private const val CHIP_PADDING_V_DP = 2
        private const val CHIP_TEXT_SP = 11f
    }
}

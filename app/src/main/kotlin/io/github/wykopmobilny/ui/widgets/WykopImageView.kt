package io.github.wykopmobilny.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.bumptech.glide.Glide
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.utils.getActivityContext

class WykopImageView(
    context: Context,
    attrs: AttributeSet?,
) : AppCompatImageView(context, attrs) {
    init {
        setOnClickListener { openImageListener() }
        scaleType = ScaleType.CENTER_CROP
    }

    var isResized = false
    var forceDisableMinimizedMode = false
    var openImageListener: () -> Unit = {}
    var onResizedListener: (Boolean) -> Unit = {}
    var showResizeView: (Boolean) -> Unit = {}

    private val settingsPreferencesApi by lazy { (context.applicationContext as WykopApp).settingsPreferencesApi.get() }
    private val screenMetrics by lazy {
        val metrics = DisplayMetrics()
        getActivityContext()!!.windowManager.defaultDisplay.getMetrics(metrics)
        metrics
    }

    // Referencja do aktywnego requestu - bez niej Glide nie potrafi anulowac
    // poprzedniego ladowania przy recyklingu wiersza i bitmapy wyciekaja.
    private var currentTarget: CustomTarget<Bitmap>? = null

    fun loadImageFromUrl(url: String) {
        clearCurrentTarget()
        // CustomTarget bez wymiarow = dekodowanie w PELNEJ rozdzielczosci zrodla
        // (SIZE_ORIGINAL) - kilkanascie MP na wpis konczy sie OOM-em na urzadzeniach.
        // Limit ekranu (x2 wysokosci dla dlugich obrazkow) ogranicza downsampling.
        val target =
            object : CustomTarget<Bitmap>(
                screenMetrics.widthPixels,
                screenMetrics.heightPixels * 2,
            ) {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?,
                ) {
                    DiagnosticCheckpoint.log(
                        "EntryImages",
                        "decoded ${resource.width}x${resource.height} (${resource.byteCount / 1024}KB) from ${url.substringAfterLast('/')}",
                    )
                    setImageBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Bitmapa moze wrocic do puli Glide - nie wolno jej dalej wyswietlac.
                    setImageBitmap(null)
                }
            }
        currentTarget = target
        Glide
            .with(context)
            .asBitmap()
            .load(url)
            .apply(
                RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    // AT_MOST: tylko pomniejszanie. Domyslne CENTER_OUTSIDE SKALUJE W GORE
                    // male zrodla (np. wariant w400) do rozmiaru targetu - 400px robilo sie 4800px.
                    .downsample(DownsampleStrategy.AT_MOST)
                    .signature(ObjectKey(url)),
            ).into(target)
    }

    fun resetImage() {
        clearCurrentTarget()
        setImageBitmap(null)
    }

    private fun clearCurrentTarget() {
        currentTarget?.let { Glide.with(context).clear(it) }
        currentTarget = null
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val targetHeightPercentage = (settingsPreferencesApi.cutImageProportion ?: DEFAULT_CUT_IMAGE_PROPORTION).toFloat() / 100
        val proportion = (screenMetrics.heightPixels.toFloat() * targetHeightPercentage) / screenMetrics.widthPixels.toFloat()
        val widthSpec =
            MeasureSpec.getSize(
                if (settingsPreferencesApi.showMinifiedImages && !forceDisableMinimizedMode) widthMeasureSpec / 2 else widthMeasureSpec,
            )
        if (drawable != null) {
            val measuredMultiplier = (drawable.intrinsicHeight.toFloat() / drawable.intrinsicWidth.toFloat())
            val heightSpec = widthSpec.toFloat() * measuredMultiplier
            if (measuredMultiplier > proportion && !isResized && settingsPreferencesApi.cutImages) {
                setOnClickListener {
                    isResized = true
                    onResizedListener(true)
                    requestLayout()
                    invalidate()
                    setOnClickListener { openImageListener() }
                }
                setMeasuredDimension(widthSpec, (widthSpec.toFloat() * proportion).toInt())
                showResizeView(true)
            } else {
                setMeasuredDimension(widthSpec, heightSpec.toInt())
                setOnClickListener { openImageListener() }
                showResizeView(false)
            }
        } else {
            setMeasuredDimension(widthSpec, (widthSpec.toFloat() * proportion).toInt())
            setOnClickListener { openImageListener() }
            showResizeView(false)
        }
        if (!settingsPreferencesApi.cutImages) showResizeView(false)
    }

    companion object {
        const val DEFAULT_CUT_IMAGE_PROPORTION = 60
    }
}

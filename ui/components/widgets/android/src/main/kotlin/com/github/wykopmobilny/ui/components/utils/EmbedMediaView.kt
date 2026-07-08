package com.github.wykopmobilny.ui.components.utils

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.card.MaterialCardView
import io.github.wykopmobilny.ui.components.widgets.EmbedMediaUi
import io.github.wykopmobilny.ui.components.widgets.NSFW_PLACEHOLDER
import io.github.wykopmobilny.ui.components.widgets.PLUS18_PLACEHOLDER
import io.github.wykopmobilny.ui.components.widgets.android.R
import io.github.wykopmobilny.ui.components.widgets.android.databinding.ViewEmbedMediaBinding
import io.github.wykopmobilny.utils.bindings.setOnClick
import io.github.wykopmobilny.ui.base.android.R as BaseR

class EmbedMediaView(
    context: Context,
    attrs: AttributeSet?,
) : MaterialCardView(context, attrs) {
    init {
        inflate(context, R.layout.view_embed_media, this)
        cardElevation = 0f
        radius = context.readDimensionAttr(BaseR.attr.cornerRadius).toFloat()
        strokeWidth = context.readDimensionAttr(BaseR.attr.outlineWidth)
        setStrokeColor(context.readColorAttr(BaseR.attr.colorOutline))
    }
}

fun EmbedMediaView.bind(model: EmbedMediaUi?) {
    val binding = ViewEmbedMediaBinding.bind(this)
    binding.root.setOnClick(model?.clickAction)
    binding.root.isVisible = model != null
    model ?: return

    val overlay = model.overlay
    binding.fullOverlay.isVisible = overlay != null
    binding.imgPreview.isVisible = overlay == null
    if (overlay != null) {
        val placeholder =
            when (overlay) {
                EmbedMediaUi.Overlay.Nsfw -> NSFW_PLACEHOLDER
                EmbedMediaUi.Overlay.Plus18 -> PLUS18_PLACEHOLDER
            }
        Glide
            .with(this)
            .load(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.fullOverlay)
    }

    val url =
        when (val content = model.content) {
            is EmbedMediaUi.Content.StaticImage -> content.url
            is EmbedMediaUi.Content.PlayableMedia -> content.previewImage
        }
    // Wysokosc z adjustViewBounds po zaladowaniu bitmapy - 0dp + dimensionRatio
    // nie rozwiazuje sie wewnatrz wrap_content i widok mial zerowa wysokosc.
    Glide
        .with(this)
        .load(url)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .transition(withCrossFade())
        .into(binding.imgPreview)

    val prompt =
        when (val content = model.content) {
            is EmbedMediaUi.Content.StaticImage -> null
            is EmbedMediaUi.Content.PlayableMedia -> content.domain
        }
    binding.imgPromptBackground.isVisible = prompt != null
    binding.txtPrompt.isVisible = prompt != null
    binding.txtPrompt.text = prompt
    binding.txtSize.isVisible = model.size != null
    binding.txtSize.text = model.size
}

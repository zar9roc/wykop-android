package com.github.wykopmobilny.ui.components

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import io.github.wykopmobilny.ui.components.widgets.AvatarUi
import io.github.wykopmobilny.ui.components.widgets.android.R
import io.github.wykopmobilny.ui.components.widgets.android.databinding.ViewAvatarSimpleBinding
import io.github.wykopmobilny.utils.bindings.setOnClick
import io.github.wykopmobilny.ui.base.android.R as BaseR

class AvatarView(
    context: Context,
    attributeSet: AttributeSet?,
) : ConstraintLayout(context, attributeSet) {
    init {
        inflate(context, R.layout.view_avatar_simple, this)
    }
}

fun AvatarView.bind(model: AvatarUi?) {
    val binding = ViewAvatarSimpleBinding.bind(this)
//    val transformation = CircleCrop()

    binding.imgAvatar.setOnClick(model?.onClicked)

    if (getTag(BaseR.id.cache) == model.toString()) {
        return
    }
    val requestOptions =
        RequestOptions()
            .centerCrop()
//            .transform(transformation)

    val placeholder =
        Glide
            .with(context)
            .load(BaseR.drawable.avatar)
            .apply(requestOptions)

    // Jeśli avatarUrl jest null lub pusty, użyj tylko placeholdera
    val avatarUrl = model?.avatarUrl
    if (avatarUrl.isNullOrBlank()) {
        placeholder.into(binding.imgAvatar)
    } else {
        Glide
            .with(binding.imgAvatar)
            .load(avatarUrl)
            .apply(requestOptions)
            .thumbnail(placeholder)
            .into(binding.imgAvatar)
    }
    Glide
        .with(binding.imgGenderStrip)
        .load(
            model
                ?.genderStrip
                .toColorInt(context)
                .defaultColor
                .let(::ColorDrawable),
        ).dontAnimate()
        .into(binding.imgGenderStrip)
    setTag(BaseR.id.cache, model.toString())
}

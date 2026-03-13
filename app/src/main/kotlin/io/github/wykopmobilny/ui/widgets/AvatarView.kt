package io.github.wykopmobilny.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import io.github.wykopmobilny.databinding.AvatarViewBinding
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.utils.api.getGenderStripResource
import io.github.wykopmobilny.utils.layoutInflater
import io.github.wykopmobilny.utils.loadImage
import io.github.wykopmobilny.ui.base.android.R as BaseR

class AvatarView(
    context: Context,
    attrs: AttributeSet,
) : FrameLayout(context, attrs) {
    private val binding = AvatarViewBinding.inflate(layoutInflater, this, true)

    fun setAuthor(author: Author) {
        author.apply {
            if (avatarUrl.isBlank()) {
                binding.avatarImageView.setImageResource(BaseR.drawable.avatar)
            } else {
                binding.avatarImageView.loadImage(avatarUrl, renderParams = "q80")
            }
            binding.genderStripImageView.setBackgroundResource(getGenderStripResource(sex))
        }
    }
}

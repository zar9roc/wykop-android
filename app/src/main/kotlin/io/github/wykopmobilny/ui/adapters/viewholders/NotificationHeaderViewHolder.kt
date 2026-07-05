package io.github.wykopmobilny.ui.adapters.viewholders

import android.graphics.drawable.Drawable
import androidx.core.content.res.use
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.HashtagNotificationHeaderListItemBinding
import io.github.wykopmobilny.models.dataclass.NotificationHeader
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler

class NotificationHeaderViewHolder(
    private val binding: HashtagNotificationHeaderListItemBinding,
    private val navigator: NewNavigator,
    private val linkHandler: WykopLinkHandler,
    private val collapseListener: (Boolean, String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    val collapseDrawable: Drawable? by lazy {
        itemView.context
            .obtainStyledAttributes(arrayOf(R.attr.collapseDrawable).toIntArray())
            .use { it.getDrawable(0) }
    }

    val expandDrawable: Drawable? by lazy {
        itemView.context
            .obtainStyledAttributes(arrayOf(R.attr.expandDrawable).toIntArray())
            .use { it.getDrawable(0) }
    }

    fun bindView(tag: NotificationHeader) {
        binding.notificationTag.text = tag.title ?: "#${tag.tag}"
        binding.notificationCount.text = tag.notificationsCount.toString()
        val drawable = if (tag.visible) collapseDrawable else expandDrawable
        binding.collapseButtonImageView.setImageDrawable(drawable)
        binding.collapseButtonImageView.setOnClickListener { collapseListener(!tag.visible, tag.tag) }
        binding.root.setOnClickListener {
            val url = tag.navigationUrl
            if (url != null) {
                linkHandler.handleUrl(url)
            } else {
                navigator.openTagActivity(tag.tag.removePrefix("#"))
            }
        }
    }
}

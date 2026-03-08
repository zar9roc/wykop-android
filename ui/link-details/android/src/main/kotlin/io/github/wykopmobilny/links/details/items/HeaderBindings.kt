package io.github.wykopmobilny.links.details.items

import android.content.res.ColorStateList
import android.view.View
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.github.wykopmobilny.ui.components.StandaloneTagView
import com.github.wykopmobilny.ui.components.bind
import com.github.wykopmobilny.ui.components.setUserNick
import com.github.wykopmobilny.ui.components.toColorInt
import com.github.wykopmobilny.ui.components.utils.readColorAttr
import io.github.wykopmobilny.links.details.LinkDetailsHeaderUi
import io.github.wykopmobilny.ui.components.widgets.TagUi
import io.github.wykopmobilny.ui.link_details.android.databinding.LinkDetailsHeaderBinding
import io.github.wykopmobilny.utils.bindings.setOnClick
import androidx.appcompat.R as AppcompatR
import io.github.wykopmobilny.ui.base.android.R as BaseR

internal fun LinkDetailsHeaderBinding.bindHeader(header: LinkDetailsHeaderUi) =
    when (header) {
        LinkDetailsHeaderUi.Loading -> {
            root.isInvisible = true
        }

        is LinkDetailsHeaderUi.WithData -> {
            root.isVisible = true

            imgPreview.isVisible = header.previewImageUrl != null
            if (header.previewImageUrl != null) {
                Glide
                    .with(imgPreview)
                    .load(header.previewImageUrl)
                    .transition(withCrossFade())
                    .into(imgPreview)
            }
            imgPreview.setOnClick(header.viewLinkAction)

            txtTitle.text = header.title
            txtTitle.setOnClick(header.viewLinkAction)
            txtDescription.text = header.body
            txtDescription.setOnClick(header.viewLinkAction)

            imgAvatar.bind(header.author.avatar)
            txtUser.setUserNick(header.author)
            txtTimestamp.text = header.postedAgo
            txtDomain.text = header.domain

            tagsContainer.updateTags(header.tags)
            val hasTags = header.tags.isNotEmpty()
            tagsSeparator.isVisible = hasTags
            tagsContainer.isVisible = hasTags

            hotBadgeStrip.isVisible = header.badge != null
            hotBadgeStrip.setBackgroundColor(header.badge.toColorInt(hotBadgeStrip.context).defaultColor)

            shareButton.setOnClick(header.viewLinkAction)

            favoriteButton.isVisible = header.favoriteButton.isVisible
            favoriteButton.setOnClick(header.favoriteButton.clickAction)
            favoriteButton.setImageResource(
                if (header.favoriteButton.isToggled) {
                    BaseR.drawable.ic_favorite
                } else {
                    BaseR.drawable.ic_favorite_outlined
                },
            )
            favoriteButton.imageTintList =
                if (header.favoriteButton.isToggled) {
                    ColorStateList.valueOf(ContextCompat.getColor(favoriteButton.context, BaseR.color.favorite_enabled))
                } else {
                    favoriteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
                }

            commentButton.bind(header.commentsCount)
            voteButton.bind(header.voteCount)
            moreButton.setOnClick(header.moreAction)
            commentSortButton.bind(header.commentsSort)
            addCommentButton.setOnClick(header.addCommentAction)
            commentAvatar.bind(header.currentUser?.avatar)
        }
    }

private fun ConstraintLayout.updateTags(tags: List<TagUi>) {
    val newValue = tags.map { it.name }
    if (tag != newValue) {
        removeAllViews()
        val tagViews =
            tags.map { tag ->
                StandaloneTagView(context, tag).apply {
                    id = View.generateViewId()
                }
            }
        val flow = Flow(context)
        flow.referencedIds = tagViews.map { it.id }.toIntArray()
        flow.setWrapMode(Flow.WRAP_CHAIN)
        flow.setHorizontalStyle(Flow.CHAIN_PACKED)
        flow.setHorizontalBias(0f)
        flow.setVerticalAlign(Flow.VERTICAL_ALIGN_CENTER)
        addView(flow)
        tagViews.forEach(this::addView)
        tag = newValue
    }
}

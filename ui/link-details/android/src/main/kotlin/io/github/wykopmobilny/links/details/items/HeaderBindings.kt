package io.github.wykopmobilny.links.details.items

import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
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

internal fun LinkDetailsHeaderBinding.bindHeader(
    header: LinkDetailsHeaderUi,
    relatedCount: Int = 0,
) = when (header) {
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
        hotIcon.isVisible = header.badge != null

        favoriteButton.isVisible = header.favoriteButton.isVisible
        favoriteButton.setOnClick(header.favoriteButton.clickAction)
        val favoriteResId = if (header.favoriteButton.isToggled) {
            BaseR.drawable.ic_favorite
        } else {
            BaseR.drawable.ic_favorite_outlined
        }
        favoriteButton.setCompoundDrawablesRelativeWithIntrinsicBounds(favoriteResId, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(
            favoriteButton,
            if (header.favoriteButton.isToggled) {
                ColorStateList.valueOf(ContextCompat.getColor(favoriteButton.context, BaseR.color.favorite_enabled))
            } else {
                favoriteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
            },
        )

        // Related links button
        relatedButton.isVisible = relatedCount > 0
        relatedButton.text = relatedCount.toString()

        // Comment count — inline button
        val commentsLabel = header.commentsCount.label
        commentButton.text = commentsLabel
        val commentsColor =
            header.commentsCount.color?.toColorInt(commentButton.context)
                ?: commentButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
        commentButton.setTextColor(commentsColor)
        TextViewCompat.setCompoundDrawableTintList(commentButton, commentsColor)
        commentButton.setOnClick(header.commentsCount.clickAction)

        // Vote button — only up, color changes based on state
        val voteColor =
            header.voteCount.color?.toColorInt(voteButton.context)
                ?: voteButton.context.readColorAttr(AppcompatR.attr.colorControlNormal)
        voteButton.text = header.voteCount.count.toString()
        voteButton.setTextColor(voteColor)
        TextViewCompat.setCompoundDrawableTintList(voteButton, voteColor)
        voteButton.setOnClick(header.voteCount.upvoteAction)

        moreButton.setOnClick(header.moreAction)
    }
}

private fun LinearLayout.updateTags(tags: List<TagUi>) {
    val newValue = tags.map { it.name }
    if (tag != newValue) {
        removeAllViews()
        for (tagData in tags) {
            val tagView = StandaloneTagView(context, tagData)
            addView(tagView)
        }
        tag = newValue
    }
}

package io.github.wykopmobilny.ui.modules.links.linkdetails.items

import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.github.wykopmobilny.ui.components.toColorInt
import com.github.wykopmobilny.ui.components.utils.readColorAttr
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkDetailsHeaderLayoutBinding
import io.github.wykopmobilny.links.details.LinkDetailsHeaderUi
import io.github.wykopmobilny.ui.components.widgets.AvatarUi
import io.github.wykopmobilny.utils.bindings.setOnClick
import androidx.appcompat.R as AppcompatR

internal fun LinkDetailsHeaderLayoutBinding.bindHeaderV3(
    header: LinkDetailsHeaderUi,
    relatedCount: Int = 0,
) = when (header) {
    LinkDetailsHeaderUi.Loading -> {
        root.isInvisible = true
    }

    is LinkDetailsHeaderUi.WithData -> {
        root.isVisible = true

        // Preview image
        image.isVisible = header.previewImageUrl != null
        if (header.previewImageUrl != null) {
            Glide
                .with(image)
                .load(header.previewImageUrl)
                .transition(withCrossFade())
                .into(image)
        }
        image.setOnClick(header.viewLinkAction)

        // Title & description
        titleTextView.text = header.title
        titleTextView.setOnClick(header.viewLinkAction)
        description.text = header.body
        description.setOnClick(header.viewLinkAction)

        // Author info — bind to app AvatarView via internal ImageView
        avatarView.bindAvatarV3(header.author.avatar)
        avatarView.setOnClick(header.author.avatar.onClicked)
        userTextView.text = header.author.name
        val authorColor =
            header.author.color?.toColorInt(userTextView.context)
                ?: userTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
        userTextView.setTextColor(authorColor)

        // Date & domain
        dateTextView.text = header.postedAgo
        urlTextView.text = header.domain

        // Tags — show in blockedTextView (reused for tags display)
        val tagsText = header.tags.joinToString(" ") { "#${it.name}" }
        blockedTextView.text = tagsText
        blockedTextView.isVisible = header.tags.isNotEmpty()

        // Hot badge
        hotBadgeStrip.isVisible = header.badge != null
        if (header.badge != null) {
            hotBadgeStrip.setBackgroundColor(
                header.badge.toColorInt(hotBadgeStrip.context).defaultColor,
            )
        }

        // Favorite button
        favoriteButton.isVisible = header.favoriteButton.isVisible
        favoriteButton.isFavorite = header.favoriteButton.isToggled
        favoriteButton.setOnClickListener { header.favoriteButton.clickAction?.invoke() }

        // Related count
        relatedCountTextView.isVisible = relatedCount > 0
        relatedCountTextView.text = relatedCount.toString()

        // Comments count
        commentsCountTextView.text = header.commentsCount.label
        val commentsColor =
            header.commentsCount.color?.toColorInt(commentsCountTextView.context)
                ?: commentsCountTextView.context.readColorAttr(R.attr.textColorButtonToolbar)
        commentsCountTextView.setTextColor(commentsColor)
        commentsCountTextView.setOnClick(header.commentsCount.clickAction)

        // Vote button (DigVoteButton)
        val voteColor =
            header.voteCount.color?.toColorInt(diggCountTextView.context)
                ?: diggCountTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
        diggCountTextView.text = header.voteCount.count.toString()
        diggCountTextView.setTextColor(voteColor)
        diggCountTextView.setOnClick(header.voteCount.upvoteAction)

        // Share button
        shareTextView.setOnClick(header.viewLinkAction)

        // More options
        moreOptionsTextView.setOnClick(header.moreAction)
    }
}

internal fun android.view.View.bindAvatarV3(avatar: AvatarUi?) {
    val imageView = findViewById<ImageView>(R.id.avatarImageView) ?: return
    if (avatar?.avatarUrl != null) {
        Glide
            .with(imageView)
            .load(avatar.avatarUrl)
            .centerCrop()
            .into(imageView)
    }
    val genderStrip = findViewById<ImageView>(R.id.genderStripImageView)
    if (genderStrip != null && avatar != null) {
        val stripColor = avatar.genderStrip.toColorInt(genderStrip.context).defaultColor
        genderStrip.setBackgroundColor(stripColor)
    }
}

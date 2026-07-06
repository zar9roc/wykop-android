package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.wykopmobilny.ui.components.bind
import com.github.wykopmobilny.ui.components.toColorInt
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkRelatedItemV3Binding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.links.details.RelatedLinkUi
import io.github.wykopmobilny.ui.components.widgets.ColorConst

internal class RelatedLinksAdapter : ListAdapter<RelatedLinkUi, RelatedLinksAdapter.ViewHolder>(RelatedLinkDiff) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding =
            LinkRelatedItemV3Binding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: LinkRelatedItemV3Binding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(link: RelatedLinkUi) {
            // Author avatar and name
            val author = link.author
            binding.authorHeaderView.isVisible = author != null
            binding.userNameTextView.isVisible = author != null
            if (author != null) {
                binding.authorHeaderView.bind(author.avatar)
                binding.userNameTextView.text = author.name
                binding.userNameTextView.setTextColor(
                    author.color.toColorInt(binding.root.context),
                )
                binding.authorHeaderView.setOnClickListener {
                    author.avatar.onClicked?.invoke()
                }
                binding.userNameTextView.setOnClickListener {
                    author.avatar.onClicked?.invoke()
                }
            }

            // Title, thumbnail and URL
            binding.title.text = link.title
            // Pelny adres zamiast samej domeny - dla kompletnosci wpisu.
            binding.urlTextView.text = link.url
            binding.thumbnailImageView.isVisible = link.previewImageUrl != null
            if (link.previewImageUrl != null) {
                Glide
                    .with(binding.thumbnailImageView)
                    .load(link.previewImageUrl)
                    .centerCrop()
                    .into(binding.thumbnailImageView)
            } else {
                Glide.with(binding.thumbnailImageView).clear(binding.thumbnailImageView)
            }

            // Vote count and buttons
            val voteCount = link.upvotesCount.count
            binding.voteCountTextView.text = if (voteCount > 0) "+$voteCount" else "$voteCount"

            val userVoteColor = link.upvotesCount.color
            val voteColorRes =
                when (userVoteColor) {
                    ColorConst.CounterUpvoted -> R.color.plusPressedColor
                    ColorConst.CounterDownvoted -> R.color.minusPressedColor
                    else -> null
                }
            if (voteColorRes != null) {
                binding.voteCountTextView.setTextColor(
                    ContextCompat.getColor(binding.root.context, voteColorRes),
                )
            } else {
                // Reset to default text color when no vote
                binding.voteCountTextView.setTextColor(
                    binding.title.currentTextColor,
                )
            }

            // Set selected state on vote buttons
            binding.plusButton.isButtonSelected = userVoteColor == ColorConst.CounterUpvoted
            binding.minusButton.isButtonSelected = userVoteColor == ColorConst.CounterDownvoted

            // Plus button
            binding.plusButton.isEnabled = link.upvotesCount.upvoteAction != null
            binding.plusButton.setOnClickListener {
                DiagnosticCheckpoint.log(
                    "RelatedLinks",
                    "Upvote clicked: title=${link.title}, count=${link.upvotesCount.count}",
                )
                link.upvotesCount.upvoteAction?.invoke()
            }

            // Minus button
            binding.minusButton.isEnabled = link.upvotesCount.downvoteAction != null
            binding.minusButton.setOnClickListener {
                DiagnosticCheckpoint.log(
                    "RelatedLinks",
                    "Downvote clicked: title=${link.title}, count=${link.upvotesCount.count}",
                )
                link.upvotesCount.downvoteAction?.invoke()
            }

            // Share button
            binding.shareTextView.setOnClickListener {
                link.shareAction()
            }

            // Report button - hidden for related links
            binding.reportTextView.isVisible = false

            // Main click action
            binding.root.setOnClickListener {
                link.clickAction()
            }
        }
    }

    companion object RelatedLinkDiff : DiffUtil.ItemCallback<RelatedLinkUi>() {
        override fun areItemsTheSame(
            oldItem: RelatedLinkUi,
            newItem: RelatedLinkUi,
        ): Boolean = oldItem.title == newItem.title && oldItem.domain == newItem.domain

        override fun areContentsTheSame(
            oldItem: RelatedLinkUi,
            newItem: RelatedLinkUi,
        ) = oldItem == newItem
    }
}

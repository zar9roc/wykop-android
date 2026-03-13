package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.wykopmobilny.ui.components.bind
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkRelatedListItemBinding
import io.github.wykopmobilny.links.details.RelatedLinkUi

internal class RelatedLinksAdapter : ListAdapter<RelatedLinkUi, RelatedLinksAdapter.ViewHolder>(RelatedLinkDiff) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val binding = LinkRelatedListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
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
        private val binding: LinkRelatedListItemBinding,
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
                    author.color.toColorInt(binding.root.context)
                )
                binding.authorHeaderView.setOnClickListener {
                    author.avatar.onClicked?.invoke()
                }
                binding.userNameTextView.setOnClickListener {
                    author.avatar.onClicked?.invoke()
                }
            }

            // Title and URL
            binding.title.text = link.title
            binding.urlTextView.text = link.domain

            // Vote count and buttons
            val voteCount = link.upvotesCount.count
            binding.voteCountTextView.text = if (voteCount > 0) "+$voteCount" else "$voteCount"

            val voteColor = when {
                voteCount > 0 -> R.color.plusPressedColor
                voteCount < 0 -> R.color.minusPressedColor
                else -> null
            }
            voteColor?.let {
                binding.voteCountTextView.setTextColor(
                    ContextCompat.getColor(binding.root.context, it)
                )
            }

            // Plus button
            binding.plusButton.isEnabled = link.upvotesCount.upvoteAction != null
            binding.plusButton.setOnClickListener {
                link.upvotesCount.upvoteAction?.invoke()
            }

            // Minus button
            binding.minusButton.isEnabled = link.upvotesCount.downvoteAction != null
            binding.minusButton.setOnClickListener {
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

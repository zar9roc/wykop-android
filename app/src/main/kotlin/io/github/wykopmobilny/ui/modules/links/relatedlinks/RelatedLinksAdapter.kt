package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.R
import io.github.wykopmobilny.links.details.RelatedLinkUi

internal class RelatedLinksAdapter : ListAdapter<RelatedLinkUi, RelatedLinksAdapter.ViewHolder>(RelatedLinkDiff) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val context = parent.context
        val textView =
            TextView(context).apply {
                textSize = 14f
                setPadding(
                    context.resources.getDimensionPixelSize(R.dimen.padding_dp_large),
                    context.resources.getDimensionPixelSize(R.dimen.padding_dp_tiny),
                    context.resources.getDimensionPixelSize(R.dimen.padding_dp_large),
                    context.resources.getDimensionPixelSize(R.dimen.padding_dp_tiny),
                )
                setBackgroundResource(
                    context
                        .obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                        .let { ta -> ta.getResourceId(0, 0).also { ta.recycle() } },
                )
            }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val textView: TextView,
    ) : RecyclerView.ViewHolder(textView) {
        fun bind(link: RelatedLinkUi) {
            textView.text = "${link.title} (${link.domain})"
            textView.setOnClickListener { link.clickAction() }
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

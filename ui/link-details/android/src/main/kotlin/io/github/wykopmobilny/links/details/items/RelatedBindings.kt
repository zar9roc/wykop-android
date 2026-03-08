package io.github.wykopmobilny.links.details.items

import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.github.wykopmobilny.links.details.RelatedLinkUi
import io.github.wykopmobilny.links.details.RelatedLinksSectionUi
import io.github.wykopmobilny.ui.link_details.android.databinding.LinkDetailsRelatedBinding

internal fun LinkDetailsRelatedBinding.bindRelated(related: RelatedLinksSectionUi) {
    when (related) {
        is RelatedLinksSectionUi.Loading -> {
            root.isVisible = true
            relatedItemsContainer.removeAllViews()
        }

        is RelatedLinksSectionUi.Empty -> {
            root.isVisible = false
        }

        is RelatedLinksSectionUi.WithData -> {
            root.isVisible = true
            relatedItemsContainer.removeAllViews()
            for (link in related.links) {
                addRelatedItem(relatedItemsContainer, link)
            }
        }

        is RelatedLinksSectionUi.FullWidthError -> {
            root.isVisible = false
        }
    }
}

private fun addRelatedItem(
    container: LinearLayout,
    link: RelatedLinkUi,
) {
    val context = container.context
    val item =
        TextView(context).apply {
            text = "${link.title} (${link.domain})"
            textSize = 14f
            setPadding(0, 8, 0, 8)
            setBackgroundResource(
                context
                    .obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                    .let { ta -> ta.getResourceId(0, 0).also { ta.recycle() } },
            )
            setOnClickListener { link.clickAction() }
        }
    container.addView(item)
}

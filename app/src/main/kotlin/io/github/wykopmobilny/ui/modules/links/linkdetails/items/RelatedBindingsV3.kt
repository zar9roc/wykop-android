package io.github.wykopmobilny.ui.modules.links.linkdetails.items

import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.github.wykopmobilny.databinding.LinkDetailsRelatedSectionBinding
import io.github.wykopmobilny.links.details.RelatedLinkUi
import io.github.wykopmobilny.links.details.RelatedLinksSectionUi

internal fun LinkDetailsRelatedSectionBinding.bindRelatedSectionV3(related: RelatedLinksSectionUi) {
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
                addRelatedItemV3(relatedItemsContainer, link)
            }
        }

        is RelatedLinksSectionUi.FullWidthError -> {
            root.isVisible = false
        }
    }
}

private fun addRelatedItemV3(
    container: LinearLayout,
    link: RelatedLinkUi,
) {
    val context = container.context
    val item =
        TextView(context).apply {
            text = "${link.title} (${link.domain})"
            textSize = 14f
            setPadding(
                context.resources.getDimensionPixelSize(
                    io.github.wykopmobilny.R.dimen.padding_dp_large,
                ),
                context.resources.getDimensionPixelSize(
                    io.github.wykopmobilny.R.dimen.padding_dp_tiny,
                ),
                context.resources.getDimensionPixelSize(
                    io.github.wykopmobilny.R.dimen.padding_dp_large,
                ),
                context.resources.getDimensionPixelSize(
                    io.github.wykopmobilny.R.dimen.padding_dp_tiny,
                ),
            )
            setBackgroundResource(
                context
                    .obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                    .let { ta -> ta.getResourceId(0, 0).also { ta.recycle() } },
            )
            setOnClickListener { link.clickAction() }
        }
    container.addView(item)
}

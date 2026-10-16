package io.github.wykopmobilny.ui.adapters.viewholders

import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.databinding.LinkRelatedListItemBinding
import io.github.wykopmobilny.models.dataclass.Related
import io.github.wykopmobilny.ui.widgets.link.related.RelatedWidgetPresenter
import io.github.wykopmobilny.utils.usermanager.UserManagerApi

class RelatedViewHolder(
    private val linkId: Long?,
    private val binding: LinkRelatedListItemBinding,
    private val userManagerApi: UserManagerApi,
    private val relatedWidgetPresenter: RelatedWidgetPresenter,
) : RecyclerView.ViewHolder(binding.root) {
    /** [linkId] nadpisuje znalezisko z konstruktora - na listach mieszanych kazdy wiersz ma wlasne. */
    fun bindView(
        related: Related,
        linkId: Long? = this.linkId,
    ) {
        binding.relatedView.setRelatedData(linkId, related, userManagerApi, relatedWidgetPresenter)
    }

    companion object {
        const val TYPE_RELATED = 23
    }
}

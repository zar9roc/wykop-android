package io.github.wykopmobilny.ui.adapters.viewholders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.databinding.NotificationsLoadMoreItemBinding

/**
 * Stopka listy powiadomien - jawny przycisk "pokaz starsze". Zastepuje dawna
 * stopke-progres: nic nie dociaga sie samo, dopiero klikniecie wysyla zapytanie
 * o kolejna strone. W trakcie ladowania wiersz pokazuje progres i jest zablokowany,
 * wiec drugie tapniecie nie wyszle drugiego zapytania.
 */
class NotificationLoadMoreViewHolder(
    private val binding: NotificationsLoadMoreItemBinding,
    private val onClick: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(isLoading: Boolean) {
        binding.loadMoreProgress.isVisible = isLoading
        binding.loadMoreLabel.isVisible = !isLoading
        binding.loadMoreItem.isClickable = !isLoading
        binding.loadMoreItem.setOnClickListener { if (!isLoading) onClick() }
    }
}

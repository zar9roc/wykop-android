package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.base.adapter.EndlessProgressAdapter
import io.github.wykopmobilny.databinding.HashtagNotificationHeaderListItemBinding
import io.github.wykopmobilny.databinding.NotificationsListItemBinding
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationHeader
import io.github.wykopmobilny.ui.adapters.viewholders.NotificationHeaderViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.NotificationViewHolder
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.ui.modules.notificationslist.NotificationCollapseStorage
import io.github.wykopmobilny.utils.layoutInflater
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import javax.inject.Inject

class NotificationsListAdapter
    @Inject
    constructor(
        val navigator: NewNavigator,
        val linkHandler: WykopLinkHandler,
        private val collapseStorage: NotificationCollapseStorage,
    ) : EndlessProgressAdapter<RecyclerView.ViewHolder, Notification>() {
        companion object {
            const val TYPE_HEADER = 2123
            const val TYPE_ITEM = 2124
        }

        var itemClickListener: (Int) -> Unit = {}
        private val items: List<Notification?>
            get() = dataset.filter { it?.visible ?: true || it is NotificationHeader }

        private val updateHeader: (String) -> Unit = { tag ->
            (dataset.find { it is NotificationHeader && it.tag == tag } as? NotificationHeader)?.apply {
                notificationsCount -= 1
                notifyItemChanged(items.indexOf(this))
            }
        }

        private val collapseListener: (Boolean, String) -> Unit = { visibility, tagStr ->
            collapseStorage.setCollapsed(tag = tagStr, collapsed = !visibility)
            dataset
                .filter { it?.tag == tagStr }
                .forEach {
                    it?.visible = visibility
                }
            notifyDataSetChanged()
        }

        // Odtwarza zapamiętany stan zwinięcia po (prze)ładowaniu listy - inaczej świeże
        // obiekty Notification miałyby domyślne visible=true i akordeony byłyby rozwinięte.
        // Tylko dla tagów które mają nagłówek (tryb grupowania) - w płaskiej liście
        // pojedyncze powiadomienia nie mogą zniknąć przez zapamiętany klucz grupy.
        private fun applyCollapseState() {
            val headerTags =
                dataset
                    .filterIsInstance<NotificationHeader>()
                    .map { it.tag }
                    .filter(collapseStorage::isCollapsed)
                    .toSet()
            if (headerTags.isEmpty()) return
            dataset.forEach { notification ->
                if (notification != null && notification.tag in headerTags) {
                    notification.visible = false
                }
            }
        }

        override fun addData(
            items: List<Notification>,
            shouldClearAdapter: Boolean,
        ) {
            super.addData(items, shouldClearAdapter)
            applyCollapseState()
            notifyDataSetChanged()
        }

        override fun getViewType(position: Int) =
            if (items[position] is NotificationHeader) {
                TYPE_HEADER
            } else {
                TYPE_ITEM
            }

        fun collapseAll() {
            dataset.forEach { notification ->
                notification?.visible = false
                if (notification is NotificationHeader) collapseStorage.setCollapsed(notification.tag, collapsed = true)
            }
            notifyDataSetChanged()
        }

        fun expandAll() {
            dataset.forEach { notification ->
                notification?.visible = true
                if (notification is NotificationHeader) collapseStorage.setCollapsed(notification.tag, collapsed = false)
            }
            notifyDataSetChanged()
        }

        override fun constructViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ) = when (viewType) {
            TYPE_HEADER -> {
                NotificationHeaderViewHolder(
                    HashtagNotificationHeaderListItemBinding.inflate(parent.layoutInflater, parent, false),
                    navigator,
                    linkHandler,
                    collapseListener,
                )
            }

            TYPE_ITEM -> {
                NotificationViewHolder(
                    NotificationsListItemBinding.inflate(parent.layoutInflater, parent, false),
                    linkHandler,
                    updateHeader,
                )
            }

            else -> {
                error("unsupported type")
            }
        }

        override fun bindHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            when (holder) {
                is NotificationViewHolder -> holder.bindNotification(items[position]!!)
                is NotificationHeaderViewHolder -> holder.bindView(items[position] as NotificationHeader)
            }
        }

        override fun getItemCount(): Int = items.size
    }

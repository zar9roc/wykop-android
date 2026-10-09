package io.github.wykopmobilny.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.base.adapter.EndlessProgressAdapter
import io.github.wykopmobilny.databinding.HashtagNotificationHeaderListItemBinding
import io.github.wykopmobilny.databinding.NotificationsListItemBinding
import io.github.wykopmobilny.databinding.NotificationsLoadMoreItemBinding
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.dataclass.NotificationGroup
import io.github.wykopmobilny.models.dataclass.NotificationHeader
import io.github.wykopmobilny.ui.adapters.viewholders.NotificationGroupViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.NotificationHeaderViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.NotificationLoadMoreViewHolder
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

            // Wiersz zbiorczy zakladki "Do mnie" - jeden wpis/znalezisko = jeden wiersz.
            const val TYPE_GROUP = 2125

            // Stopka "pokaz starsze" - ostatni (pusty) element datasetu.
            const val TYPE_LOAD_MORE = 2126
        }

        var itemClickListener: (Int) -> Unit = {}

        // Klikniecie stopki. Lista nie doladowuje sie sama przy scrollu - to jedyna
        // droga do kolejnej strony.
        var loadMoreListener: () -> Unit = {}

        // true miedzy klinieciem stopki a odpowiedzia API. Blokuje kolejne
        // klikniecia, zeby podwojne tapniecie nie wyslalo dwoch zapytan.
        private var isLoadingMore = false

        private val onLoadMoreClicked: () -> Unit = {
            if (!isLoadingMore && itemCount > 0) {
                isLoadingMore = true
                notifyItemChanged(itemCount - 1)
                loadMoreListener()
            }
        }

        /**
         * Ponowna proba z snackbara bledu - ta sama sciezka co klikniecie stopki,
         * wiec obowiazuje ta sama blokada przed podwojnym zapytaniem.
         */
        fun requestLoadMore() = onLoadMoreClicked()

        /**
         * Koniec proby doladowania zakonczonej bledem - stopka wraca do stanu
         * klikalnego, zeby dalo sie sprobowac jeszcze raz.
         */
        fun finishLoadMore() {
            if (isLoadingMore) {
                isLoadingMore = false
                notifyDataSetChanged()
            }
        }

        private val items: List<Notification?>
            get() = dataset.filter { it?.visible ?: true || it is NotificationHeader }

        // Klucze grup, ktorych wiersze zbiorcze sa obecnie na liscie.
        private val groupKeys: Set<String>
            get() = dataset.filterIsInstance<NotificationGroup>().map { it.tag }.toSet()

        private val updateHeader: (String) -> Unit = { tag ->
            val row = dataset.find { it != null && (it is NotificationHeader || it is NotificationGroup) && it.tag == tag }
            when (row) {
                is NotificationHeader -> {
                    row.notificationsCount -= 1
                    notifyItemChanged(items.indexOf(row))
                }

                is NotificationGroup -> {
                    if (row.unreadCount > 0) row.unreadCount -= 1
                    row.new = row.unreadCount > 0
                    notifyItemChanged(items.indexOf(row))
                }

                else -> Unit
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

        /**
         * Chevron wiersza zbiorczego. Sam wiersz zostaje widoczny - zwijamy wylacznie
         * powiadomienia skladowe. Stan zapamietujemy odwrotnie niz dla naglowkow tagow,
         * bo grupy "Do mnie" sa domyslnie ZWINIETE.
         */
        private val groupCollapseListener: (Boolean, String) -> Unit = { expanded, groupKey ->
            collapseStorage.setExpanded(groupKey = groupKey, expanded = expanded)
            dataset.forEach {
                if (it != null && it !is NotificationGroup && it.tag == groupKey) {
                    it.visible = expanded
                }
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
            val groups = groupKeys
            if (headerTags.isEmpty() && groups.isEmpty()) return
            dataset.forEach { notification ->
                if (notification == null || notification is NotificationGroup) return@forEach
                when (notification.tag) {
                    in headerTags -> notification.visible = false
                    // Zakladka "Do mnie": brak wpisu w pamieci = grupa zwinieta.
                    in groups -> notification.visible = collapseStorage.isExpanded(notification.tag)
                    else -> Unit
                }
            }
        }

        override fun addData(
            items: List<Notification>,
            shouldClearAdapter: Boolean,
        ) {
            isLoadingMore = false
            super.addData(items, shouldClearAdapter)
            applyCollapseState()
            notifyDataSetChanged()
        }

        override fun getViewType(position: Int) =
            when (items[position]) {
                null -> TYPE_LOAD_MORE
                is NotificationHeader -> TYPE_HEADER
                is NotificationGroup -> TYPE_GROUP
                else -> TYPE_ITEM
            }

        /**
         * Bazowa implementacja rozpoznaje stopke po `dataset`, a ten ekran renderuje
         * `items` (dataset bez pozycji ukrytych w zwinietych grupach). Przy zwinietej
         * grupie indeksy sie rozjezdzaly i stopka dostawala typ zwyklego wiersza.
         */
        override fun getItemViewType(position: Int) = getViewType(position)

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) = bindHolder(holder, position)

        fun collapseAll() {
            val groups = groupKeys
            if (groups.isEmpty()) {
                // Zakladka tagow - zachowanie bez zmian.
                dataset.forEach { notification ->
                    notification?.visible = false
                    if (notification is NotificationHeader) collapseStorage.setCollapsed(notification.tag, collapsed = true)
                }
            } else {
                dataset.forEach { notification ->
                    when {
                        notification == null -> Unit
                        notification is NotificationGroup -> collapseStorage.setExpanded(notification.tag, expanded = false)
                        notification.tag in groups -> notification.visible = false
                        else -> Unit
                    }
                }
            }
            notifyDataSetChanged()
        }

        fun expandAll() {
            val groups = groupKeys
            if (groups.isEmpty()) {
                // Zakladka tagow - zachowanie bez zmian.
                dataset.forEach { notification ->
                    notification?.visible = true
                    if (notification is NotificationHeader) collapseStorage.setCollapsed(notification.tag, collapsed = false)
                }
            } else {
                dataset.forEach { notification ->
                    when {
                        notification == null -> Unit
                        notification is NotificationGroup -> collapseStorage.setExpanded(notification.tag, expanded = true)
                        notification.tag in groups -> notification.visible = true
                        else -> Unit
                    }
                }
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

            TYPE_GROUP -> {
                NotificationGroupViewHolder(
                    NotificationsListItemBinding.inflate(parent.layoutInflater, parent, false),
                    linkHandler,
                    groupCollapseListener,
                )
            }

            TYPE_LOAD_MORE -> {
                NotificationLoadMoreViewHolder(
                    NotificationsLoadMoreItemBinding.inflate(parent.layoutInflater, parent, false),
                    onLoadMoreClicked,
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
                is NotificationLoadMoreViewHolder -> holder.bind(isLoadingMore)

                is NotificationGroupViewHolder -> {
                    val group = items[position] as NotificationGroup
                    holder.bindGroup(group, expanded = collapseStorage.isExpanded(group.tag))
                }

                is NotificationViewHolder -> items[position]?.let(holder::bindNotification)
                is NotificationHeaderViewHolder -> holder.bindView(items[position] as NotificationHeader)
            }
        }

        override fun getItemCount(): Int = items.size
    }

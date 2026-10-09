package io.github.wykopmobilny.ui.adapters.viewholders

import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.NotificationsListItemBinding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.models.dataclass.NotificationGroup
import io.github.wykopmobilny.models.dataclass.NotificationTargetKind
import io.github.wykopmobilny.utils.api.getGroupColor
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.textview.removeHtml
import io.github.wykopmobilny.utils.toPrettyDate

/**
 * Wiersz zbiorczy zakladki "Do mnie" - jeden wpis/znalezisko w jednym wierszu.
 * Uzywa tego samego layoutu co zwykly wiersz, ale doklada licznik nieprzeczytanych
 * i chevron zwijania; sam naglowek-akordeon tutaj nie wystepuje.
 */
class NotificationGroupViewHolder(
    private val binding: NotificationsListItemBinding,
    private val linkHandler: WykopLinkHandler,
    // (rozwiniete, klucz grupy)
    private val collapseListener: (Boolean, String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private val collapseDrawable: Drawable? by lazy {
        itemView.context.obtainStyledAttributes(intArrayOf(R.attr.collapseDrawable)).use { it.getDrawable(0) }
    }

    private val expandDrawable: Drawable? by lazy {
        itemView.context.obtainStyledAttributes(intArrayOf(R.attr.expandDrawable)).use { it.getDrawable(0) }
    }

    fun bindGroup(
        group: NotificationGroup,
        expanded: Boolean,
    ) {
        binding.apply {
            val text = buildText(group)
            body.setText(text.removeHtml(), TextView.BufferType.SPANNABLE)
            date.text = group.date?.toPrettyDate()

            val author = group.author
            avatarView.isVisible = author != null
            if (author != null) {
                avatarView.setAuthor(author)
                if (author.nick.isNotEmpty()) {
                    (body.text as Spannable).setSpan(
                        ForegroundColorSpan(getGroupColor(author.group)),
                        0,
                        // Format "{nick} {akcja}" - kolorujemy pierwszy wyraz, jak w zwyklym wierszu.
                        text.substringBefore(" ").length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
            }

            applyUnreadState(group)

            // Chevron w dol = zwiniete, w gore = rozwiniete.
            groupCollapseButton.isVisible = true
            groupCollapseButton.setImageDrawable(if (expanded) collapseDrawable else expandDrawable)
            groupCollapseButton.setOnClickListener { collapseListener(!expanded, group.tag) }

            // Klikniecie wiersza = klikniecie najstarszego nieprzeczytanego powiadomienia grupy.
            // Bez uzytecznego celu wiersz nie udaje klikalnego (chevron zwijania dziala dalej).
            val target = group.url?.takeIf { it.isNotBlank() }
            if (target == null) {
                notificationItem.setOnClickListener(null)
                notificationItem.isClickable = false
            } else {
                notificationItem.isClickable = true
                notificationItem.setOnClickListener {
                    if (group.unreadCount > 0) {
                        group.unreadCount -= 1
                        group.navigationTarget.new = false
                        group.new = group.unreadCount > 0
                        applyUnreadState(group)
                    }
                    DiagnosticCheckpoint.log("NotificationGroupClick", "key=${group.tag} url=$target")
                    linkHandler.handleUrl(target)
                }
            }
        }
    }

    private fun applyUnreadState(group: NotificationGroup) {
        binding.apply {
            groupUnreadCount.isVisible = group.unreadCount > 0
            groupUnreadCount.text = group.unreadCount.toString()
            unreadLine.isVisible = group.unreadCount > 0
            unreadMark.isVisible = group.unreadCount > 0
            unreadDotMark.isVisible = group.unreadCount > 0
        }
    }

    private fun buildText(group: NotificationGroup): String {
        val resources = itemView.resources
        val subject =
            if (group.othersCount > 0) {
                resources.getString(
                    R.string.notification_group_subject_many,
                    group.nick,
                    resources.getQuantityString(R.plurals.notification_group_others, group.othersCount, group.othersCount),
                )
            } else {
                // Cala grupa od jednej osoby - liczebnik bylby mylacy.
                resources.getString(R.string.notification_group_subject_one, group.nick)
            }
        // Tresc celu (wpis: tresc, znalezisko: tytul), a gdy API jej nie przyslalo -
        // slug. Gdy nie ma ani jednego, ani drugiego - wariant bez cudzyslowu,
        // zeby nigdy nie pokazac pustego cudzyslowu.
        val title = group.targetTitle?.takeIf { it.isNotBlank() } ?: group.targetSlug?.takeIf { it.isNotBlank() }
        return when (group.targetKind) {
            NotificationTargetKind.LINK ->
                if (title == null) {
                    resources.getString(R.string.notification_group_in_link_unknown, subject)
                } else {
                    resources.getString(R.string.notification_group_in_link, subject, title)
                }

            else ->
                if (title == null) {
                    resources.getString(R.string.notification_group_in_entry_unknown, subject)
                } else {
                    resources.getString(R.string.notification_group_in_entry, subject, title)
                }
        }
    }
}

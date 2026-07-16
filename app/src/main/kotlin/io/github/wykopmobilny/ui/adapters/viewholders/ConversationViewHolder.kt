package io.github.wykopmobilny.ui.adapters.viewholders

import android.graphics.Typeface
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.databinding.ConversationListItemBinding
import io.github.wykopmobilny.models.dataclass.Conversation
import io.github.wykopmobilny.ui.modules.pm.conversation.ConversationActivity
import io.github.wykopmobilny.utils.api.getGroupColor

class ConversationViewHolder(
    private val binding: ConversationListItemBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bindView(conversation: Conversation) {
        // Nieprzeczytana rozmowa: pogrubiony nick + zajawka.
        val typeface = if (conversation.unread) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        binding.authorAvatarView.setAuthor(conversation.user)
        binding.userNameTextView.apply {
            text = conversation.user.nick
            setTextColor(context.getGroupColor(conversation.user.group))
            setTypeface(typeface)
        }
        binding.lastMessageTextView.apply {
            isVisible = !conversation.lastMessagePreview.isNullOrBlank()
            text = conversation.lastMessagePreview
            setTypeface(typeface)
        }
        binding.entryDateTextView.text = conversation.lastUpdate
        binding.root.setOnClickListener { it.context.startActivity(ConversationActivity.createIntent(it.context, conversation.user.nick)) }
    }
}

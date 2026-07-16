package io.github.wykopmobilny.ui.modules.pm.conversation

import io.github.wykopmobilny.base.BaseView
import io.github.wykopmobilny.models.dataclass.FullConversation
import io.github.wykopmobilny.models.dataclass.PMMessage

interface ConversationView : BaseView {
    fun showConversation(conversation: FullConversation)

    // Optymistyczne doklejenie wiadomości zwróconej z 201 po wysłaniu -
    // pełny reload leci równolegle jako uzgodnienie.
    fun appendMessage(message: PMMessage)

    fun hideInputToolbar()

    fun resetInputbarState()

    fun hideInputbarProgress()
}

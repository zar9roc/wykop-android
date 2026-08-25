package io.github.wykopmobilny.ui.modules.pm.conversation

import io.github.wykopmobilny.base.BaseView
import io.github.wykopmobilny.models.dataclass.FullConversation
import io.github.wykopmobilny.models.dataclass.PMMessage

interface ConversationView : BaseView {
    fun showConversation(conversation: FullConversation)

    // Optymistyczne doklejenie wiadomości zwróconej z 201 po wysłaniu.
    fun appendMessage(message: PMMessage)

    // Starsze wiadomości dociągnięte przez prev_message (infinite scroll w górę) -
    // trafiają na koniec listy adaptera (wizualnie u góry, przy reverseLayout).
    fun prependOlderMessages(messages: List<PMMessage>)

    // Nowe wiadomości rozmówcy dociągnięte przez next_message - na początek listy
    // adaptera (wizualnie na dole).
    fun appendNewMessages(messages: List<PMMessage>)

    fun hideInputToolbar()

    fun resetInputbarState()

    fun hideInputbarProgress()
}

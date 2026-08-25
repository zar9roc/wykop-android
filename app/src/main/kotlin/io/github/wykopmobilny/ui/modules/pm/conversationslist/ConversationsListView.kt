package io.github.wykopmobilny.ui.modules.pm.conversationslist

import io.github.wykopmobilny.base.BaseView
import io.github.wykopmobilny.models.dataclass.Conversation

interface ConversationsListView : BaseView {
    // Pierwsza strona (lub wynik wyszukiwania) - zastepuje liste.
    fun showConversations(conversations: List<Conversation>)

    // Kolejna strona (infinite scroll) - doklejana na koniec listy.
    fun appendConversations(conversations: List<Conversation>)
}

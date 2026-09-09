package io.github.wykopmobilny.api.pm

import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.responses.ConversationDeleteResponse
import io.github.wykopmobilny.models.dataclass.Conversation
import io.github.wykopmobilny.models.dataclass.FullConversation
import io.github.wykopmobilny.models.dataclass.PMMessage
import io.reactivex.Single

interface PMApi {
    // page = strona (od 1); query = filtr po nicku (min 3 znaki; null = wszystkie).
    fun getConversations(
        page: Int = 1,
        query: String? = null,
    ): Single<List<Conversation>>

    // prevMessage/nextMessage = key wiadomosci granicznej; oba null = pelna rozmowa (od najnowszych).
    fun getConversation(
        user: String,
        prevMessage: String? = null,
        nextMessage: String? = null,
    ): Single<FullConversation>

    // Czy rozmówca napisał coś nowego od ostatniego pobrania rozmowy.
    fun hasNewerMessages(user: String): Single<Boolean>

    fun deleteConversation(user: String): Single<ConversationDeleteResponse>

    fun sendMessage(
        body: String,
        user: String,
        embed: String?,
        plus18: Boolean,
        embedUrl: String? = null,
    ): Single<PMMessage>

    fun sendMessage(
        body: String,
        user: String,
        plus18: Boolean,
        embed: WykopImageFile,
        embedUrl: String? = null,
    ): Single<PMMessage>
}

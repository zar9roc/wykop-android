package io.github.wykopmobilny.ui.modules.pm.conversation

import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.pm.PMApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.PMMessage
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ConversationPresenter
    @Inject
    constructor(
        val schedulers: Schedulers,
        private val pmApi: PMApi,
    ) : BasePresenter<ConversationView>() {
        lateinit var user: String

        // Wiadomosci przychodza z API w kolejnosci od najstarszej do najnowszej:
        // messages.first() = najstarsza (prev_message), messages.last() = najnowsza (next_message).
        private var oldestKey: String? = null
        private var newestKey: String? = null
        private var isLoadingOlder = false
        private var hasMoreOlder = true

        private companion object {
            const val NEWER_POLL_INTERVAL_S = 15L
        }

        // Wywolywane przy kazdym pelnym pokazaniu rozmowy (poczatkowe ladowanie,
        // swipe-refresh, przywrocenie stanu) - seeduje klucze graniczne.
        fun onConversationShown(messages: List<PMMessage>) {
            oldestKey = messages.firstOrNull()?.key
            newestKey = messages.lastOrNull()?.key
            hasMoreOlder = messages.isNotEmpty()
        }

        // Nowe wiadomosci rozmowcy pojawiaja sie na zywo: lekki odpyt
        // /pm/conversations/{user}/newer co kilkanascie sekund, a gdy serwer potwierdzi
        // nowsze - dociagniecie TYLKO nowych przez next_message (nie caly watek).
        fun startListeningForNewMessages() {
            Observable
                .interval(NEWER_POLL_INTERVAL_S, NEWER_POLL_INTERVAL_S, TimeUnit.SECONDS)
                .flatMapSingle {
                    pmApi
                        .hasNewerMessages(user)
                        .subscribeOn(schedulers.backgroundThread())
                        .onErrorReturnItem(false)
                }.observeOn(schedulers.mainThread())
                .subscribe { hasNewer ->
                    if (hasNewer) loadNewerMessages()
                }.intoComposite(compositeObservable)
        }

        fun loadConversation() {
            pmApi
                .getConversation(user)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { view?.showConversation(it) },
                    { view?.showErrorDialog(it) },
                ).intoComposite(compositeObservable)
        }

        // Infinite scroll w gore: dociaga wiadomosci starsze niz najstarsza widoczna.
        fun loadOlderMessages() {
            val prev = oldestKey ?: return
            if (isLoadingOlder || !hasMoreOlder) return
            isLoadingOlder = true
            pmApi
                .getConversation(user, prevMessage = prev)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { older ->
                        isLoadingOlder = false
                        val messages = older.messages
                        if (messages.isEmpty()) {
                            hasMoreOlder = false
                        } else {
                            oldestKey = messages.firstOrNull()?.key ?: oldestKey
                            view?.prependOlderMessages(messages)
                        }
                    },
                    {
                        isLoadingOlder = false
                        view?.showErrorDialog(it)
                    },
                ).intoComposite(compositeObservable)
        }

        // Dociaga tylko wiadomosci nowsze niz ostatnio znana (po potwierdzeniu przez /newer).
        private fun loadNewerMessages() {
            val next = newestKey ?: run {
                loadConversation()
                return
            }
            pmApi
                .getConversation(user, nextMessage = next)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { newer ->
                        val messages = newer.messages
                        if (messages.isNotEmpty()) {
                            newestKey = messages.lastOrNull()?.key ?: newestKey
                            view?.appendNewMessages(messages)
                        }
                    },
                    { view?.showErrorDialog(it) },
                ).intoComposite(compositeObservable)
        }

        fun sendMessage(
            body: String,
            photo: String?,
            containsAdultContent: Boolean,
            embedUrl: String? = null,
        ) {
            pmApi
                .sendMessage(body, user, photo, containsAdultContent, embedUrl)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { sent -> onMessageSent(sent) },
                    {
                        view?.hideInputbarProgress()
                        view?.showErrorDialog(it)
                    },
                ).intoComposite(compositeObservable)
        }

        fun sendMessage(
            body: String,
            photo: WykopImageFile,
            containsAdultContent: Boolean,
            embedUrl: String? = null,
        ) {
            pmApi
                .sendMessage(body, user, containsAdultContent, photo, embedUrl)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { sent -> onMessageSent(sent) },
                    {
                        view?.hideInputbarProgress()
                        view?.showErrorDialog(it)
                    },
                ).intoComposite(compositeObservable)
        }

        private fun onMessageSent(sent: PMMessage) {
            view?.hideInputbarProgress()
            view?.resetInputbarState()
            // Wiadomosc z odpowiedzi 201 pokazujemy od razu; aktualizujemy tez newestKey,
            // zeby polling next_message nie sciagnal jej ponownie (bez pelnego reloadu).
            sent.key?.let { newestKey = it }
            view?.appendMessage(sent)
        }
    }

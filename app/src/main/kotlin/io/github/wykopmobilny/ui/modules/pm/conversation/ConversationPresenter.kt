package io.github.wykopmobilny.ui.modules.pm.conversation

import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.pm.PMApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
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

        private companion object {
            const val NEWER_POLL_INTERVAL_S = 15L
        }

        // Nowe wiadomości rozmówcy pojawiają się na żywo: lekki odpyt
        // /pm/conversations/{user}/newer co kilkanaście sekund, pełne pobranie
        // tylko gdy serwer potwierdzi nowsze. Subskrypcja żyje w composite -
        // unsubscribe() przy zamknięciu ekranu zatrzymuje polling.
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
                    if (hasNewer) loadConversation()
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

        fun sendMessage(
            body: String,
            photo: String?,
            containsAdultContent: Boolean,
        ) {
            pmApi
                .sendMessage(body, user, photo, containsAdultContent)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { sent ->
                        view?.hideInputbarProgress()
                        view?.resetInputbarState()
                        // Wiadomość z odpowiedzi 201 pokazujemy od razu; reload
                        // w tle uzgadnia stan (embedy, kolejność).
                        view?.appendMessage(sent)
                        loadConversation()
                    },
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
        ) {
            pmApi
                .sendMessage(body, user, containsAdultContent, photo)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { sent ->
                        view?.hideInputbarProgress()
                        view?.resetInputbarState()
                        // Wiadomość z odpowiedzi 201 pokazujemy od razu; reload
                        // w tle uzgadnia stan (embedy, kolejność).
                        view?.appendMessage(sent)
                        loadConversation()
                    },
                    {
                        view?.hideInputbarProgress()
                        view?.showErrorDialog(it)
                    },
                ).intoComposite(compositeObservable)
        }
    }

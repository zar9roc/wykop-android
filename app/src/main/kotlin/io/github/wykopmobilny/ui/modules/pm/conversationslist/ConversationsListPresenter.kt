package io.github.wykopmobilny.ui.modules.pm.conversationslist

import io.github.wykopmobilny.api.pm.PMApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.utils.intoComposite

class ConversationsListPresenter(
    private val schedulers: Schedulers,
    private val pmApi: PMApi,
) : BasePresenter<ConversationsListView>() {
    private var page = 1
    private var hasMore = true
    private var isLoading = false

    // Filtr po nicku rozmowcy (min 3 znaki); null = pelna lista.
    private var query: String? = null

    private companion object {
        const val MIN_QUERY_LENGTH = 3
    }

    // Pierwsza strona lub nowe wyszukiwanie - resetuje paginacje i zastepuje liste.
    fun loadConversations(rawQuery: String? = query) {
        query = rawQuery?.trim()?.takeIf { it.length >= MIN_QUERY_LENGTH }
        page = 1
        hasMore = true
        isLoading = true
        pmApi
            .getConversations(page = page, query = query)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { conversations ->
                    isLoading = false
                    hasMore = conversations.isNotEmpty()
                    view?.showConversations(conversations)
                },
                {
                    isLoading = false
                    view?.showErrorDialog(it)
                },
            ).intoComposite(compositeObservable)
    }

    // Kolejna strona (infinite scroll). Pusta strona = koniec listy.
    fun loadMore() {
        if (isLoading || !hasMore) return
        isLoading = true
        val nextPage = page + 1
        pmApi
            .getConversations(page = nextPage, query = query)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { conversations ->
                    isLoading = false
                    if (conversations.isEmpty()) {
                        hasMore = false
                    } else {
                        page = nextPage
                        view?.appendConversations(conversations)
                    }
                },
                {
                    isLoading = false
                    view?.showErrorDialog(it)
                },
            ).intoComposite(compositeObservable)
    }
}

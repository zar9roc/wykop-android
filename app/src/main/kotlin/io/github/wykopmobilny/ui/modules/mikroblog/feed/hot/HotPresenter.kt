package io.github.wykopmobilny.ui.modules.mikroblog.feed.hot

import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.utils.intoComposite

class HotPresenter(
    val schedulers: Schedulers,
    val entriesApi: EntriesApi,
) : BasePresenter<HotView>() {
    var page: String? = null
    var period = "24"

    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) page = null
        val success: (FilteredData<Entry>) -> Unit = { data ->
            if (data.totalCount > 0) {
                page = data.nextPage
                view?.showHotEntries(data.filtered, shouldRefresh)
            } else {
                view?.disableLoading()
            }
        }
        val failure: (Throwable) -> Unit = {
            view?.showErrorDialog(it)
        }

        when (period) {
            "24", "12", "6" -> {
                entriesApi
                    .getHot(page, period)
                    .subscribeOn(schedulers.backgroundThread())
                    .observeOn(schedulers.mainThread())
                    .subscribe(success, failure)
            }

            "active" -> {
                entriesApi
                    .getActive(page)
                    .subscribeOn(schedulers.backgroundThread())
                    .observeOn(schedulers.mainThread())
                    .subscribe(success, failure)
            }

            else -> {
                // Newest
                entriesApi
                    .getStream(page)
                    .subscribeOn(schedulers.backgroundThread())
                    .observeOn(schedulers.mainThread())
                    .subscribe(success, failure)
            }
        }.intoComposite(compositeObservable)
    }
}

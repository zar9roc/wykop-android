package io.github.wykopmobilny.ui.modules.input.entry.edit

import io.github.wykopmobilny.api.PhotoSource
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.ui.modules.input.InputPresenter
import io.github.wykopmobilny.utils.intoComposite

class EditEntryPresenter(
    private val schedulers: Schedulers,
    private val entriesApi: EntriesApi,
) : InputPresenter<EditEntryView>() {
    override fun send(
        photos: List<PhotoSource>,
        containsAdultContent: Boolean,
        embedUrl: String?,
    ) {
        view?.showProgressBar = true
        val body = view?.textBody ?: return
        val entryId = view?.entryId ?: return

        entriesApi
            .editEntry(
                body = body,
                entryId = entryId,
                photos = photos,
                plus18 = containsAdultContent,
                embedUrl = embedUrl,
                survey = view?.surveyKey,
            ).subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { view?.exitActivity() },
                {
                    view?.showProgressBar = false
                    view?.showErrorDialog(it)
                },
            ).intoComposite(compositeObservable)
    }
}

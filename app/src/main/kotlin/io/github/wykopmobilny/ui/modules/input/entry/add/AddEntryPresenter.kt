package io.github.wykopmobilny.ui.modules.input.entry.add

import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.ui.modules.input.InputPresenter
import io.github.wykopmobilny.utils.intoComposite
import javax.inject.Inject

class AddEntryPresenter
    @Inject
    constructor(
        private val schedulers: Schedulers,
        private val entriesApi: EntriesApi,
    ) : InputPresenter<AddEntryActivityView>() {
        // Id ankiety utworzonej wczesniej (POST /v3/entries/survey), doklejane do wpisu przy
        // wysylce. Ustawiane/czyszczone przez AddEntryActivity; null = brak ankiety.
        var pendingSurveyId: String? = null

        override fun sendWithPhoto(
            photo: WykopImageFile,
            containsAdultContent: Boolean,
        ) {
            view?.showProgressBar = true
            entriesApi
                .addEntry(view?.textBody!!, photo, containsAdultContent, pendingSurveyId)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { view?.openEntryActivity(it.id) },
                    {
                        view?.showProgressBar = false
                        view?.showErrorDialog(it)
                    },
                ).intoComposite(compositeObservable)
        }

        override fun sendWithPhotoUrl(
            photo: String?,
            containsAdultContent: Boolean,
        ) {
            view?.showProgressBar = true
            entriesApi
                .addEntry(view?.textBody!!, photo, containsAdultContent, pendingSurveyId)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { view?.openEntryActivity(it.id) },
                    {
                        view?.showProgressBar = false
                        view?.showErrorDialog(it)
                    },
                ).intoComposite(compositeObservable)
        }

        // Tworzy ankiete na serwerze; po sukcesie zapamietuje survey_id i pokazuje podglad.
        fun createSurvey(
            question: String,
            answers: List<String>,
        ) {
            entriesApi
                .createSurvey(question, answers)
                .subscribeOn(schedulers.backgroundThread())
                .observeOn(schedulers.mainThread())
                .subscribe(
                    { surveyId ->
                        pendingSurveyId = surveyId
                        view?.onSurveyCreated(question, answers)
                    },
                    { view?.showErrorDialog(it) },
                ).intoComposite(compositeObservable)
        }
    }

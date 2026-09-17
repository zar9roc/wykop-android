package io.github.wykopmobilny.ui.modules.profile.microblog.comments

import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.profile.ProfileApi
import io.github.wykopmobilny.base.BasePresenter
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.ui.fragments.entries.EntriesInteractor
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.github.wykopmobilny.utils.intoComposite
import io.reactivex.Single

class MicroblogCommentsPresenter(
    val schedulers: Schedulers,
    val profileApi: ProfileApi,
    val entriesApi: EntriesApi,
    private val entryCommentInteractor: EntryCommentInteractor,
    private val entriesInteractor: EntriesInteractor,
) : BasePresenter<MicroblogCommentsView>(),
    EntryCommentActionListener,
    EntryActionListener {
    var page = 1
    lateinit var username: String

    fun loadData(shouldRefresh: Boolean) {
        if (shouldRefresh) page = 1
        profileApi
            .getEntriesComments(username, page)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    if (it.isNotEmpty()) {
                        page++
                        view?.addItems(it, shouldRefresh)
                    } else {
                        view?.disableLoading()
                        if (shouldRefresh) {
                            view?.showSearchEmptyView = true
                        }
                    }
                },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    override fun voteComment(comment: EntryComment) = entryCommentInteractor.voteComment(comment).processEntryCommentSingle(comment)

    override fun unvoteComment(comment: EntryComment) = entryCommentInteractor.unvoteComment(comment).processEntryCommentSingle(comment)

    override fun deleteComment(comment: EntryComment) = entryCommentInteractor.deleteComment(comment).processEntryCommentSingle(comment)

    override fun getVoters(comment: EntryComment) {
        view?.openVotersMenu()
        entriesApi
            .getEntryCommentVoters(comment.entryId, comment.id)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                {
                    view?.showVoters(it)
                },
                {
                    view?.showErrorDialog(it)
                },
            ).intoComposite(compositeObservable)
    }

    override fun voteEntry(entry: Entry) = entriesInteractor.voteEntry(entry).processEntrySingle(entry)

    override fun unvoteEntry(entry: Entry) = entriesInteractor.unvoteEntry(entry).processEntrySingle(entry)

    override fun markFavorite(entry: Entry) = entriesInteractor.markFavorite(entry).processEntrySingle(entry)

    override fun deleteEntry(entry: Entry) = entriesInteractor.deleteEntry(entry).processEntrySingle(entry)

    override fun observeDiscussion(entry: Entry) = entriesInteractor.observeDiscussion(entry).processEntrySingle(entry)

    override fun voteSurvey(
        entry: Entry,
        index: Int,
    ) = entriesInteractor.voteSurvey(entry, index).processEntrySingle(entry)

    override fun getVoters(entry: Entry) {
        view?.openVotersMenu()
        entriesApi
            .getEntryVoters(entry.id)
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { view?.showVoters(it) },
                { view?.showErrorDialog(it) },
            ).intoComposite(compositeObservable)
    }

    private fun Single<Entry>.processEntrySingle(entry: Entry) {
        this
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { view?.updateEntry(it) },
                {
                    view?.showErrorDialog(it)
                    view?.updateEntry(entry)
                },
            ).intoComposite(compositeObservable)
    }

    private fun Single<EntryComment>.processEntryCommentSingle(comment: EntryComment) {
        this
            .subscribeOn(schedulers.backgroundThread())
            .observeOn(schedulers.mainThread())
            .subscribe(
                { view?.updateComment(it) },
                {
                    view?.showErrorDialog(it)
                    view?.updateComment(comment)
                },
            ).intoComposite(compositeObservable)
    }
}

package io.github.wykopmobilny.ui.adapters

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentActionListener
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentInteractor
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentViewListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentActionListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentInteractor
import io.github.wykopmobilny.ui.adapters.viewholders.EntryListener
import io.github.wykopmobilny.ui.fragments.entrycomments.EntryCommentViewListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Akcje wierszy "najlepszych komentarzy" pokazywanych pod wpisem na listach.
 *
 * Listy (feed, tag, profil) nie maja prezentera od komentarzy, wiec glosowanie idzie
 * wprost przez [EntryCommentInteractor], a akcje wymagajace ekranu wpisu (odpowiedz,
 * cytat, lista plusujacych, usuwanie) przenosza na ten wpis - z komentarzem jako
 * kotwica i gotowa trescia w polu odpowiedzi.
 */
class TopCommentsActionListener(
    private val interactor: EntryCommentInteractor,
    private val navigator: NewNavigator,
    private val disposables: CompositeDisposable,
    private val onCommentUpdated: (EntryComment) -> Unit,
) : EntryCommentActionListener {
    override fun voteComment(comment: EntryComment) = interactor.voteComment(comment).apply()

    override fun unvoteComment(comment: EntryComment) = interactor.unvoteComment(comment).apply()

    // Usuwanie i lista plusujacych maja wlasny UI na ekranie wpisu - tam kierujemy.
    override fun deleteComment(comment: EntryComment) = navigator.openEntryDetailsActivity(comment.entryId, comment.id)

    override fun getVoters(comment: EntryComment) = navigator.openEntryDetailsActivity(comment.entryId, comment.id)

    private fun io.reactivex.Single<EntryComment>.apply() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { onCommentUpdated(it) },
                { Napier.w("Nie udalo sie zaglosowac na komentarz", it) },
            ).let(disposables::add)
    }
}

/**
 * "Odpowiedz"/"Cytuj" pod komentarzem na liscie - nie ma tu paska odpowiedzi, wiec
 * otwieramy ekran wpisu z gotowa trescia i fokusem na polu.
 */
class TopCommentsViewListener(
    private val navigator: NewNavigator,
) : EntryCommentViewListener {
    override fun addReply(comment: EntryComment) =
        navigator.openEntryDetailsAndReply(comment.entryId, comment.id, comment.author.nick)

    override fun quoteComment(comment: EntryComment) =
        navigator.openEntryDetailsAndQuote(comment.entryId, comment.id, comment.author.nick, comment.body)

    // Dotyczy tylko paska odpowiedzi na ekranie wpisu - na listach wpis ma wlasne
    // listenery (entryReplyListener/entryQuoteListener ponizej).
    override fun addReplyToAuthor(author: Author) = Unit
}

/**
 * "Odpowiedz" pod WPISEM na liscie - jak dla komentarza, tylko bez kotwicy:
 * ekran wpisu otwiera sie normalnie od gory, z adresatem w polu odpowiedzi.
 */
fun entryReplyListener(navigator: NewNavigator): EntryListener =
    { entry ->
        navigator.openEntryDetailsAndReply(
            entryId = entry.id,
            commentId = null,
            replyToAuthor = entry.author.nick,
        )
    }

/** "Cytuj" pod WPISEM na liscie - ekran wpisu z wklejonym cytatem jego tresci. */
fun entryQuoteListener(navigator: NewNavigator): EntryListener =
    { entry ->
        navigator.openEntryDetailsAndQuote(
            entryId = entry.id,
            commentId = null,
            quoteAuthor = entry.author.nick,
            quoteBody = entry.body,
        )
    }

/**
 * Akcje komentarzy znalezisk na listach mieszanych (profil). Glosowanie przez
 * [LinkCommentInteractor]; usuwanie ma potwierdzenie i UI na ekranie znaleziska.
 */
class ListLinkCommentActionListener(
    private val interactor: LinkCommentInteractor,
    private val navigator: NewNavigator,
    private val disposables: CompositeDisposable,
    private val onCommentUpdated: (LinkCommentV3Item) -> Unit,
) : LinkCommentActionListener {
    override fun digComment(comment: LinkCommentV3Item) = interactor.commentVoteUp(comment).apply()

    override fun buryComment(comment: LinkCommentV3Item) = interactor.commentVoteDown(comment).apply()

    override fun removeVote(comment: LinkCommentV3Item) = interactor.commentVoteCancel(comment).apply()

    override fun deleteComment(comment: LinkCommentV3Item) = navigator.openLinkDetailsActivity(comment.linkId, comment.id)

    private fun io.reactivex.Single<LinkCommentV3Item>.apply() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { onCommentUpdated(it) },
                { Napier.w("Nie udalo sie zaglosowac na komentarz znaleziska", it) },
            ).let(disposables::add)
    }
}

/**
 * "Odpowiedz"/"Cytuj" pod komentarzem znaleziska na liscie - pasek odpowiedzi jest na
 * ekranie znaleziska, wiec tam przenosimy (zakotwiczone na komentarzu).
 */
class ListLinkCommentViewListener(
    private val navigator: NewNavigator,
) : LinkCommentViewListener {
    override fun replyComment(comment: LinkCommentV3Item) = navigator.openLinkDetailsActivity(comment.linkId, comment.id)

    override fun quoteComment(comment: LinkCommentV3Item) = navigator.openLinkDetailsActivity(comment.linkId, comment.id)

    override fun setCollapsed(
        comment: LinkCommentV3Item,
        isCollapsed: Boolean,
    ) = Unit
}

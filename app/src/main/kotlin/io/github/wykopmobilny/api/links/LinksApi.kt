package io.github.wykopmobilny.api.links

import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.entries.FilteredData
import io.github.wykopmobilny.models.dataclass.Downvoter
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.models.dataclass.LinkComment
import io.github.wykopmobilny.models.dataclass.LinkVoteResponsePublishModel
import io.github.wykopmobilny.models.dataclass.Related
import io.github.wykopmobilny.models.dataclass.Upvoter
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

interface LinksApi {
    val burySubject: PublishSubject<LinkVoteResponsePublishModel>
    val digSubject: PublishSubject<LinkVoteResponsePublishModel>
    val voteRemoveSubject: PublishSubject<LinkVoteResponsePublishModel>

    fun getPromoted(page: String? = null): Single<FilteredData<Link>>

    fun getUpcoming(
        page: String? = null,
        sortBy: String,
    ): Single<FilteredData<Link>>

    fun getObserved(page: String? = null): Single<FilteredData<Link>>

    fun getLinkComments(
        linkId: Long,
        sortBy: String,
    ): Single<List<LinkComment>>

    fun getLink(linkId: Long): Single<Link>

    fun commentVoteUp(
        linkId: Long,
        commentId: Long,
    ): Single<Unit>

    fun commentVoteDown(
        linkId: Long,
        commentId: Long,
    ): Single<Unit>

    fun relatedVoteUp(
        linkId: Long,
        relatedId: Int,
    ): Single<Unit>

    fun relatedVoteDown(
        linkId: Long,
        relatedId: Int,
    ): Single<Unit>

    fun commentVoteCancel(
        linkId: Long,
        commentId: Long,
    ): Single<Unit>

    fun commentDelete(
        linkId: Long,
        commentId: Long,
    ): Single<Unit>

    fun commentAdd(
        body: String,
        plus18: Boolean,
        inputStream: WykopImageFile,
        linkId: Long,
        linkComment: Long,
    ): Single<LinkComment>

    fun relatedAdd(
        title: String,
        url: String,
        plus18: Boolean,
        linkId: Long,
    ): Single<Unit>

    fun commentAdd(
        body: String,
        embed: String?,
        plus18: Boolean,
        linkId: Long,
        linkComment: Long,
    ): Single<LinkComment>

    fun commentAdd(
        body: String,
        plus18: Boolean,
        inputStream: WykopImageFile,
        linkId: Long,
    ): Single<LinkComment>

    fun commentAdd(
        body: String,
        embed: String?,
        plus18: Boolean,
        linkId: Long,
    ): Single<LinkComment>

    fun commentEdit(
        body: String,
        linkId: Long,
        commentId: Long,
    ): Single<Unit>

    fun voteUp(
        linkId: Long,
        notifyPublisher: Boolean = true,
    ): Single<Unit>

    fun voteDown(
        linkId: Long,
        reason: Int,
        notifyPublisher: Boolean = true,
    ): Single<Unit>

    fun voteRemove(
        linkId: Long,
        notifyPublisher: Boolean = true,
    ): Single<Unit>

    fun getUpvoters(linkId: Long): Single<List<Upvoter>>

    fun getDownvoters(linkId: Long): Single<List<Downvoter>>

    fun markFavorite(
        linkId: Long,
        currentlyFavorite: Boolean,
    ): Single<Unit>

    fun getRelated(linkId: Long): Single<List<Related>>
}

package io.github.wykopmobilny.domain.repositories

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import io.github.wykopmobilny.api.endpoints.v3.FavouritesV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.favourites.FavouriteRequestV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.cache.api.UserVote
import io.github.wykopmobilny.domain.linkdetails.VoteDownReason
import io.github.wykopmobilny.domain.profile.LinkInfo
import io.github.wykopmobilny.kotlin.AppDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class LinksRepository
    @Inject
    constructor(
        private val linkStore: Store<Long, LinkInfo>,
        private val linksV3Api: LinksV3RetrofitApi,
        private val favouritesV3Api: FavouritesV3RetrofitApi,
        private val appCache: AppCache,
    ) {
        suspend fun toggleFavorite(
            linkId: Long,
            currentlyFavorite: Boolean,
        ) {
            val request =
                WykopApiRequestV3(
                    FavouriteRequestV3(type = "link", sourceId = linkId),
                )
            if (currentlyFavorite) {
                favouritesV3Api.removeFavourite(request)
            } else {
                favouritesV3Api.addFavourite(request)
            }
            linkStore.fresh(linkId)
        }

        suspend fun toggleCommentFavorite(
            linkId: Long,
            commentId: Long,
            currentlyFavorite: Boolean,
        ) {
            val request =
                WykopApiRequestV3(
                    FavouriteRequestV3(type = "link_comment", sourceId = commentId),
                )
            if (currentlyFavorite) {
                favouritesV3Api.removeFavourite(request)
            } else {
                favouritesV3Api.addFavourite(request)
            }
            withContext(AppDispatchers.IO) {
                appCache.linkCommentsQueries.favorite(
                    linkId = linkId,
                    id = commentId,
                    isFavorite = !currentlyFavorite,
                )
            }
        }

        suspend fun voteUp(linkId: Long) {
            linksV3Api.voteUp(linkId)
            withContext(AppDispatchers.IO) {
                appCache.linksQueries.voteUpOptimistic(
                    id = linkId,
                    userVote = UserVote.Up,
                )
            }
        }

        suspend fun removeVote(linkId: Long) {
            linksV3Api.removeVote(linkId)
            withContext(AppDispatchers.IO) {
                appCache.linksQueries.removeVoteOptimistic(
                    id = linkId,
                    wasUp = UserVote.Up,
                    wasDown = UserVote.Down,
                )
            }
        }

        suspend fun voteDown(
            linkId: Long,
            reason: VoteDownReason,
        ) {
            linksV3Api.voteDown(linkId, reason.apiValue)
            withContext(AppDispatchers.IO) {
                appCache.linksQueries.voteDownOptimistic(
                    id = linkId,
                    userVote = UserVote.Down,
                )
            }
        }

        suspend fun commentVoteUp(
            linkId: Long,
            commentId: Long,
        ) {
            linksV3Api.voteComment(linkId = linkId, commentId = commentId, type = "up")
            withContext(AppDispatchers.IO) {
                appCache.linkCommentsQueries.voteUpOptimistic(
                    id = commentId,
                    linkId = linkId,
                    userVote = UserVote.Up,
                )
            }
        }

        suspend fun commentVoteDown(
            linkId: Long,
            commentId: Long,
        ) {
            linksV3Api.voteComment(linkId = linkId, commentId = commentId, type = "down")
            withContext(AppDispatchers.IO) {
                appCache.linkCommentsQueries.voteDownOptimistic(
                    id = commentId,
                    linkId = linkId,
                    userVote = UserVote.Down,
                )
            }
        }

        suspend fun removeCommentVote(
            linkId: Long,
            commentId: Long,
        ) {
            linksV3Api.removeCommentVote(linkId = linkId, commentId = commentId)
            withContext(AppDispatchers.IO) {
                appCache.linkCommentsQueries.removeVoteOptimistic(
                    id = commentId,
                    linkId = linkId,
                    wasUp = UserVote.Up,
                    wasDown = UserVote.Down,
                )
            }
        }

        suspend fun relatedVoteUp(
            linkId: Long,
            relatedId: Long,
        ) {
            linksV3Api.voteRelated(linkId = linkId, relatedId = relatedId, type = "up")
            withContext(AppDispatchers.IO) {
                appCache.linksRelatedQueries.voteUpOptimistic(
                    id = relatedId,
                    linkId = linkId,
                    userVote = UserVote.Up,
                )
            }
        }

        suspend fun relatedVoteDown(
            linkId: Long,
            relatedId: Long,
        ) {
            linksV3Api.voteRelated(linkId = linkId, relatedId = relatedId, type = "down")
            withContext(AppDispatchers.IO) {
                appCache.linksRelatedQueries.voteDownOptimistic(
                    id = relatedId,
                    linkId = linkId,
                    userVote = UserVote.Down,
                )
            }
        }
    }

private val VoteDownReason.apiValue: Int
    get() =
        when (this) {
            VoteDownReason.Duplicate -> 1
            VoteDownReason.Spam -> 2
            VoteDownReason.FakeInfo -> 3
            VoteDownReason.WrongContent -> 4
            VoteDownReason.UnsuitableContent -> 5
        }

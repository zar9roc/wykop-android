package io.github.wykopmobilny.api.links

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.endpoints.LinksRetrofitApi
import io.github.wykopmobilny.api.entries.allowImageOnly
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.exceptions.handleMediaUpload
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateCommentRequestV3
import io.github.wykopmobilny.api.requests.v3.links.AddRelatedRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import io.github.wykopmobilny.models.dataclass.LinkVoteResponsePublishModel
import io.github.wykopmobilny.models.mapper.apiv3.filterLinkV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinksV3
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinksRepository
    @Inject
    constructor(
        private val linksApi: LinksRetrofitApi,
        private val linksApiV3: io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi,
        private val mediaApiV3: io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi,
        private val favouritesApiV3: io.github.wykopmobilny.api.endpoints.v3.FavouritesV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
    ) : LinksApi {
        override val voteRemoveSubject = PublishSubject.create<LinkVoteResponsePublishModel>()
        override val digSubject = PublishSubject.create<LinkVoteResponsePublishModel>()
        override val burySubject = PublishSubject.create<LinkVoteResponsePublishModel>()

        override fun getPromoted(page: String?) =
            rxSingle { linksApiV3.getLinks(page = page, type = "homepage", sort = "newest") }
                .retryWhen(userTokenRefresher)
                .map { response ->
                    response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
                }

        override fun getUpcoming(
            page: String?,
            sortBy: String,
        ) = rxSingle { linksApiV3.getLinks(page = page, type = "upcoming", sort = sortBy) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
            }

        override fun getObserved(page: String?) =
            rxSingle { favouritesApiV3.getFavourites(page) }
                .retryWhen(userTokenRefresher)
                .map { response ->
                    val links =
                        response.data
                            .orEmpty()
                            .filterIsInstance<ObservedItemV3.LinkItem>()
                            .map { it.link }
                    links.filterLinksV3(owmContentFilter, response.pagination)
                }

        override fun getLinkComments(
            linkId: Long,
            sortBy: String,
        ) = rxSingle { linksApiV3.getLinkComments(linkId, sortBy) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<LinkCommentResponseV3>>())
            .map { comments ->
                comments.map { commentResponse ->
                    io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3.map(
                        commentResponse,
                        owmContentFilter,
                        linkId,
                    )
                }
            }.map { list ->
                list.forEach { comment ->
                    if (
                        comment.parentId == null || comment.id == comment.parentId
                    ) {
                        comment.childCommentCount = list.filter { item -> comment.id == item.parentId }.size - 1
                    }
                }
                list
            }

        override fun getLink(linkId: Long) =
            rxSingle { linksApiV3.getLink(linkId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<LinkResponseV3>())
                .map { link ->
                    link.filterLinkV3(owmContentFilter)
                }

        override fun commentVoteUp(
            linkId: Long,
            commentId: Long,
        ) = rxSingle { linksApiV3.voteComment(linkId, commentId, "up") }
            .retryWhen(userTokenRefresher)
            .map { }

        override fun commentVoteDown(
            linkId: Long,
            commentId: Long,
        ) = rxSingle { linksApiV3.voteComment(linkId, commentId, "down") }
            .retryWhen(userTokenRefresher)
            .map { }

        override fun relatedVoteUp(
            linkId: Long,
            relatedId: Int,
        ) = rxSingle { linksApiV3.voteRelated(linkId, relatedId.toLong(), "up") }
            .retryWhen(userTokenRefresher)
            .map { }

        override fun relatedVoteDown(
            linkId: Long,
            relatedId: Int,
        ) = rxSingle { linksApiV3.voteRelated(linkId, relatedId.toLong(), "down") }
            .retryWhen(userTokenRefresher)
            .map { }

        override fun commentVoteCancel(
            linkId: Long,
            commentId: Long,
        ) = rxSingle { linksApiV3.removeCommentVote(linkId, commentId) ?: WykopApiResponseV3(data = Unit, pagination = null) }
            .retryWhen(userTokenRefresher)
            .map { }

        override fun voteUp(
            linkId: Long,
            notifyPublisher: Boolean,
        ) = rxSingle { linksApiV3.voteUp(linkId) }
            .retryWhen(userTokenRefresher)
            .map { }
            .doOnSuccess {
                if (notifyPublisher) {
                    digSubject.onNext(LinkVoteResponsePublishModel(linkId, "dig"))
                }
            }

        override fun voteDown(
            linkId: Long,
            reason: Int,
            notifyPublisher: Boolean,
        ) = rxSingle { linksApiV3.voteDown(linkId, reason) }
            .retryWhen(userTokenRefresher)
            .map { }
            .doOnSuccess {
                if (notifyPublisher) {
                    burySubject.onNext(LinkVoteResponsePublishModel(linkId, "bury"))
                }
            }

        override fun voteRemove(
            linkId: Long,
            notifyPublisher: Boolean,
        ) = rxSingle { linksApiV3.removeVote(linkId) ?: WykopApiResponseV3(data = Unit, pagination = null) }
            .retryWhen(userTokenRefresher)
            .map { }
            .doOnSuccess {
                if (notifyPublisher) {
                    voteRemoveSubject.onNext(LinkVoteResponsePublishModel(linkId, null))
                }
            }

        override fun commentAdd(
            body: String,
            embed: String?,
            plus18: Boolean,
            linkId: Long,
        ) = rxSingle {
            linksApiV3.addLinkComment(
                linkId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body,
                        embed = embed,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<LinkCommentResponseV3>())
            .map { commentV3 ->
                io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3.map(
                    commentV3,
                    owmContentFilter,
                    linkId,
                )
            }

        override fun relatedAdd(
            title: String,
            url: String,
            plus18: Boolean,
            linkId: Long,
        ) = rxSingle {
            linksApiV3.addRelated(
                linkId,
                WykopApiRequestV3(
                    AddRelatedRequestV3(
                        title = title,
                        url = url,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .map { }

        override fun commentAdd(
            body: String,
            plus18: Boolean,
            inputStream: WykopImageFile,
            linkId: Long,
        ) = rxSingle {
            val photoKey = uploadPhotoAndGetKey(inputStream)

            linksApiV3.addLinkComment(
                linkId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body.allowImageOnly(),
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<LinkCommentResponseV3>())
            .map { commentV3 ->
                io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3.map(
                    commentV3,
                    owmContentFilter,
                    linkId,
                )
            }

        override fun commentAdd(
            body: String,
            embed: String?,
            plus18: Boolean,
            linkId: Long,
            linkComment: Long,
        ) = rxSingle {
            linksApiV3.addLinkComment(
                linkId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body,
                        embed = embed,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<LinkCommentResponseV3>())
            .map { commentV3 ->
                io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3.map(
                    commentV3,
                    owmContentFilter,
                    linkId,
                )
            }

        override fun commentAdd(
            body: String,
            plus18: Boolean,
            inputStream: WykopImageFile,
            linkId: Long,
            linkComment: Long,
        ) = rxSingle {
            val photoKey = uploadPhotoAndGetKey(inputStream)

            linksApiV3.addLinkComment(
                linkId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body.allowImageOnly(),
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<LinkCommentResponseV3>())
            .map { commentV3 ->
                io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3.map(
                    commentV3,
                    owmContentFilter,
                    linkId,
                )
            }

        override fun commentEdit(
            body: String,
            linkId: Long,
            commentId: Long,
        ) = rxSingle {
            linksApiV3.editLinkComment(
                linkId,
                commentId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(content = body),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .map { }

        override fun commentDelete(
            linkId: Long,
            commentId: Long,
        ) = rxSingle { linksApiV3.deleteLinkComment(linkId, commentId) ?: WykopApiResponseV3(data = Unit, pagination = null) }
            .retryWhen(userTokenRefresher)
            .map { }

        override fun getDownvoters(linkId: Long) =
            rxSingle { linksApiV3.getDownvoters(linkId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<UserShortResponseV3>>())
                .map { users ->
                    users.map { userResponse ->
                        io.github.wykopmobilny.models.dataclass.Downvoter(
                            author =
                                io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
                                    .map(userResponse),
                            date = "",
                            reason = 0,
                        )
                    }
                }

        override fun getUpvoters(linkId: Long) =
            rxSingle { linksApiV3.getUpvoters(linkId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<UserShortResponseV3>>())
                .map { users ->
                    users.map { userResponse ->
                        io.github.wykopmobilny.models.dataclass.Upvoter(
                            author =
                                io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
                                    .map(userResponse),
                            date = "",
                        )
                    }
                }

        override fun getRelated(linkId: Long) =
            rxSingle { linksApiV3.getRelated(linkId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<RelatedResponseV3>>())
                .map { related ->
                    related.map { relatedResponse ->
                        io.github.wykopmobilny.models.mapper.apiv3.RelatedMapperV3
                            .map(relatedResponse)
                    }
                }

        override fun markFavorite(
            linkId: Long,
            currentlyFavorite: Boolean,
        ) = rxSingle {
            val request =
                WykopApiRequestV3(
                    io.github.wykopmobilny.api.requests.v3.favourites.FavouriteRequestV3(
                        type = "link",
                        sourceId = linkId,
                    ),
                )
            if (currentlyFavorite) {
                favouritesApiV3.removeFavourite(request)
            } else {
                favouritesApiV3.addFavourite(request)
            }
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>())
            .map { }

        /**
         * Helper function to upload a photo and extract its key.
         * Reduces code duplication across add/edit operations.
         */
        private suspend fun uploadPhotoAndGetKey(wykopImageFile: WykopImageFile): String? {
            val uploadedPhoto =
                handleMediaUpload {
                    mediaApiV3.uploadPhoto(wykopImageFile.getFileMultipartForV3())
                }
            return uploadedPhoto.key
        }
    }

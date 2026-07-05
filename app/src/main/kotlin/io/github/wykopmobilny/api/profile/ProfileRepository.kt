package io.github.wykopmobilny.api.profile

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.ObserveStateResponse
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkCommentResponseV3
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.models.dataclass.EntryComment
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.models.dataclass.Related
import io.github.wykopmobilny.models.mapper.apiv3.EntryCommentMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.RelatedMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntriesV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinksV3
import io.github.wykopmobilny.models.mapper.apiv3.toEntryLink
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class ProfileRepository
    @Inject
    constructor(
        private val profileApiV3: ProfileV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
        private val appStorage: AppStorage,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : ProfileApi {
        override fun getIndex(username: String): Single<UserFullResponseV3> =
            rxSingle { profileApiV3.getUserProfile(username) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<UserFullResponseV3>(errorBodyParser))

        override fun getAdded(
            username: String,
            page: String?,
        ) = rxSingle { profileApiV3.getUserLinksAdded(username, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
            }

        override fun getActions(username: String): Single<List<EntryLink>> =
            rxSingle { profileApiV3.getUserActions(username) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<ObservedItemV3>>(errorBodyParser))
                .map { items ->
                    // Mieszane wpisy i znaleziska - kazdy element mapowany na wlasny
                    // szablon (Entry albo Link) wg dyskryminatora resource.
                    items.map { it.toEntryLink(owmContentFilter) }
                }

        override fun getPublished(
            username: String,
            page: String?,
        ) = rxSingle { profileApiV3.getUserLinksPublished(username, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
            }

        override fun getEntries(
            username: String,
            page: Int,
        ) = rxSingle { profileApiV3.getUserEntriesAdded(username, page) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<EntryResponseV3>>(errorBodyParser))
            .map { it.filterEntriesV3(owmContentFilter = owmContentFilter) }

        override fun getEntriesComments(
            username: String,
            page: Int,
        ): Single<List<EntryComment>> =
            rxSingle { profileApiV3.getUserEntriesCommented(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<EntryResponseV3>>(errorBodyParser))
                .map { entries ->
                    entries.flatMap { entry ->
                        entry.comments.items
                            .orEmpty()
                            .map { comment -> EntryCommentMapperV3.map(comment, owmContentFilter, entryId = entry.id) }
                    }
                }

        override fun getLinkComments(
            username: String,
            page: Int,
        ): Single<List<LinkCommentV3Item>> =
            rxSingle { profileApiV3.getUserLinksCommented(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<LinkCommentResponseV3>>(errorBodyParser))
                .map {
                    it.map { response ->
                        val linkId =
                            response.resource
                                .substringAfter("/links/")
                                .substringBefore("/")
                                .toLongOrNull() ?: 0L
                        LinkCommentMapperV3.map(response, owmContentFilter, linkId)
                    }
                }

        override fun getBuried(
            username: String,
            page: String?,
        ) = rxSingle { profileApiV3.getUserLinksDown(username, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
            }

        override fun getDigged(
            username: String,
            page: String?,
        ) = rxSingle { profileApiV3.getUserLinksUp(username, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterLinksV3(owmContentFilter, response.pagination)
            }

        override fun getBadges(
            username: String,
            page: Int,
        ): Single<List<BadgeResponseV3>> =
            rxSingle { profileApiV3.getUserBadges(username) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<BadgeResponseV3>>(errorBodyParser))

        override fun getRelated(
            username: String,
            page: Int,
        ): Single<List<Related>> =
            rxSingle { profileApiV3.getUserLinksRelated(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<RelatedResponseV3>>(errorBodyParser))
                .map { it.map { response -> RelatedMapperV3.map(response) } }

        override fun observe(tag: String) =
            rxSingle { profileApiV3.observeUser(tag) }
                .retryWhen(userTokenRefresher)
                .map { ObserveStateResponse(isObserved = true, isBlocked = false) }

        override fun unobserve(tag: String) =
            rxSingle { profileApiV3.unobserveUser(tag) ?: WykopApiResponseV3(data = Unit, pagination = null) }
                .retryWhen(userTokenRefresher)
                .map { ObserveStateResponse(isObserved = false, isBlocked = false) }

        override fun block(tag: String) =
            rxSingle { profileApiV3.blockUser(tag) }
                .retryWhen(userTokenRefresher)
                .map {
                    appStorage.blacklistQueries.insertOrReplaceProfile(tag.removePrefix("@"))
                    ObserveStateResponse(isObserved = false, isBlocked = true)
                }

        override fun unblock(tag: String) =
            rxSingle { profileApiV3.unblockUser(tag) ?: WykopApiResponseV3(data = Unit, pagination = null) }
                .retryWhen(userTokenRefresher)
                .map {
                    appStorage.blacklistQueries.deleteProfile(tag.removePrefix("@"))
                    ObserveStateResponse(isObserved = false, isBlocked = false)
                }
    }

package io.github.wykopmobilny.api.tag

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.TagRetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.TagsV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformer
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.ObservedTagResponse
import io.github.wykopmobilny.api.responses.TagMetaResponse
import io.github.wykopmobilny.api.responses.v3.tags.TagDetailsResponseV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.models.dataclass.TagEntries
import io.github.wykopmobilny.models.dataclass.TagLinks
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinkV3
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class TagRepository
    @Inject
    constructor(
        private val tagApi: TagRetrofitApi,
        private val tagsApiV3: TagsV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
        private val appStorage: AppStorage,
    ) : TagApi {
        override fun getTagEntries(
            tag: String,
            page: String?,
        ) = rxSingle { tagsApiV3.getTagEntries(tag, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                TagEntries(
                    entries = response.data.orEmpty().map { it.filterEntryV3(owmContentFilter) },
                    nextPage = response.pagination?.next,
                )
            }

        override fun getTagLinks(
            tag: String,
            page: String?,
        ) = rxSingle { tagsApiV3.getTagLinks(tag, page) }
            .retryWhen(userTokenRefresher)
            .map { response ->
                TagLinks(
                    entries = response.data.orEmpty().map { it.filterLinkV3(owmContentFilter) },
                    nextPage = response.pagination?.next,
                )
            }

        override fun getTagDetails(tag: String): Single<TagMetaResponse> =
            rxSingle { tagsApiV3.getTagDetails(tag) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<TagDetailsResponseV3>())
                .map { details ->
                    TagMetaResponse(
                        tag = details.name,
                        isObserved = details.follow ?: false,
                        isBlocked = details.blacklist ?: false,
                        isOwn = details.personal ?: false,
                        description = details.description,
                        background = details.media?.photo?.url,
                    )
                }

        override fun getObservedTags(): Single<List<ObservedTagResponse>> =
            rxSingle { tagApi.getObservedTags() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun observe(tag: String) =
            rxSingle { tagApi.observe(tag) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun unobserve(tag: String) =
            rxSingle { tagApi.unobserve(tag) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun block(tag: String) =
            rxSingle { tagApi.block(tag) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())
                .flatMap { response ->
                    rxSingle {
                        appStorage.blacklistQueries.insertOrReplaceTag(tag.removePrefix("#"))
                        response
                    }
                }

        override fun unblock(tag: String) =
            rxSingle { tagApi.unblock(tag) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())
                .flatMap { response ->
                    rxSingle {
                        appStorage.blacklistQueries.deleteTag(tag.removePrefix("#"))
                        response
                    }
                }
    }

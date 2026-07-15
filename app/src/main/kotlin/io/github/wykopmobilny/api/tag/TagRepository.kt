package io.github.wykopmobilny.api.tag

import io.github.wykopmobilny.api.requests.v3.blacklist.BlacklistTagRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.TagsV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.responses.ObserveStateResponse
import io.github.wykopmobilny.api.responses.ObservedTagResponse
import io.github.wykopmobilny.api.responses.TagMetaResponse
import io.github.wykopmobilny.api.responses.v3.tags.TagDetailsResponseV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.models.dataclass.TagEntries
import io.github.wykopmobilny.models.dataclass.TagLinks
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinkV3
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class TagRepository
    @Inject
    constructor(
        private val tagsApiV3: TagsV3RetrofitApi,
        private val profileApiV3: ProfileV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
        private val appStorage: AppStorage,
        private val userManager: UserManagerApi,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : TagApi {
        override fun getTagEntries(
            tag: String,
            page: String?,
            sort: String?,
            year: Int?,
            month: Int?,
        ) = rxSingle { tagsApiV3.getTagEntries(tag, page, sort, year, month) }
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
            sort: String?,
            year: Int?,
            month: Int?,
        ) = rxSingle { tagsApiV3.getTagLinks(tag, page, sort, year, month) }
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
                .compose(ErrorHandlerTransformerV3<TagDetailsResponseV3>(errorBodyParser))
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

        override fun getObservedTags(): Single<List<ObservedTagResponse>> {
            val username =
                userManager.getUserCredentials()?.login
                    ?: return Single.just(emptyList())
            return rxSingle { profileApiV3.getObservedTagsMenu(username) }
                .retryWhen(userTokenRefresher)
                .map { response ->
                    response.data.orEmpty().map { tag ->
                        ObservedTagResponse(tag = tag.name)
                    }
                }
        }

        override fun observe(tag: String) =
            rxSingle { tagsApiV3.observeTag(tag).requireStateChanged() }
                .retryWhen(userTokenRefresher)
                .map { ObserveStateResponse(isObserved = true, isBlocked = false) }

        override fun unobserve(tag: String) =
            rxSingle { tagsApiV3.unobserveTag(tag).requireStateChanged() }
                .retryWhen(userTokenRefresher)
                .map { ObserveStateResponse(isObserved = false, isBlocked = false) }

        override fun block(tag: String) =
            rxSingle {
                tagsApiV3
                    .blockTag(WykopApiRequestV3(BlacklistTagRequestV3(tag = tag.removePrefix("#"))))
                    .requireStateChanged()
            }.retryWhen(userTokenRefresher)
                .map {
                    appStorage.blacklistQueries.insertOrReplaceTag(tag.removePrefix("#"))
                    ObserveStateResponse(isObserved = false, isBlocked = true)
                }

        override fun unblock(tag: String) =
            rxSingle { tagsApiV3.unblockTag(tag).requireStateChanged() }
                .retryWhen(userTokenRefresher)
                .map {
                    appStorage.blacklistQueries.deleteTag(tag.removePrefix("#"))
                    ObserveStateResponse(isObserved = false, isBlocked = false)
                }
    }

// 409 = stan juz osiagniety (np. tag juz obserwowany po rozjechaniu sie UI) -
// traktujemy jak sukces, zeby przycisk moc sie zsynchronizowac.
private fun Response<Unit>.requireStateChanged() {
    if (!isSuccessful && code() != 409) throw HttpException(this)
}

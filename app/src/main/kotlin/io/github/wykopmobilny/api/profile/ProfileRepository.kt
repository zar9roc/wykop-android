package io.github.wykopmobilny.api.profile

import io.github.wykopmobilny.api.ErrorBodyParserV3
import retrofit2.HttpException
import io.github.wykopmobilny.api.requests.v3.blacklist.BlacklistUserRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
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
import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.models.dataclass.EntryListRow
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.mapper.apiv3.EntryCommentMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.LinkCommentMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.RelatedMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntriesV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.github.wykopmobilny.models.mapper.apiv3.filterLinksV3
import io.github.wykopmobilny.models.mapper.apiv3.toEntryLink
import io.reactivex.Single
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.models.mapper.apiv3.filterLinkV3
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class ProfileRepository
    @Inject
    constructor(
        private val profileApiV3: ProfileV3RetrofitApi,
        private val linksApiV3: LinksV3RetrofitApi,
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

        override fun getActions(
            username: String,
            page: Int,
        ): Single<List<EntryLink>> =
            rxSingle { profileApiV3.getUserActions(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<ObservedItemV3>>(errorBodyParser))
                .flatMap { items ->
                    // Mieszane wpisy i znaleziska - kazdy element mapowany na wlasny
                    // szablon (Entry albo Link) wg dyskryminatora resource. Komentarze
                    // uzytkownika pod znaleziskiem ida jako wiersze pod nim (wpisy maja
                    // je w entry.comments, rozwijane przez adapter).
                    rxSingle {
                        items.map { item ->
                            item.toEntryLink(owmContentFilter).apply {
                                if (item is ObservedItemV3.LinkItem) {
                                    rows = userLinkCommentRows(item.link.id, item.link.comments.items.orEmpty())
                                }
                            }
                        }
                    }
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

        override fun getEntriesVoted(
            username: String,
            page: Int,
        ) = rxSingle { profileApiV3.getUserEntriesVoted(username, page) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<EntryResponseV3>>(errorBodyParser))
            .map { it.filterEntriesV3(owmContentFilter = owmContentFilter) }

        override fun getEntriesComments(
            username: String,
            page: Int,
        ): Single<List<EntryListRow>> =
            rxSingle { profileApiV3.getUserEntriesCommented(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<EntryResponseV3>>(errorBodyParser))
                .map { entries ->
                    // Endpoint oddaje WPISY z komentarzami uzytkownika w srodku, wiec lista
                    // jest mieszana: wpis, a zaraz pod nim jego komentarz(e) - bez
                    // dodatkowych zapytan.
                    entries.flatMap { response ->
                        // comments.items renderujemy jako osobne wiersze, wiec zdejmujemy je
                        // z wpisu - inaczej podglad "najlepszych komentarzy" dublowalby tresc.
                        val entry = response.filterEntryV3(owmContentFilter).also { it.comments.clear() }
                        val comments =
                            response.comments.items
                                .orEmpty()
                                .map { comment ->
                                    EntryCommentMapperV3.map(
                                        comment,
                                        owmContentFilter,
                                        entryId = response.id,
                                        entryAuthorNick = response.author.username,
                                    )
                                }
                        listOf(EntryListRow.EntryRow(entry)) +
                            comments.map(EntryListRow::CommentRow)
                    }
                }

        override fun getLinkComments(
            username: String,
            page: Int,
        ): Single<List<EntryLink>> =
            rxSingle { profileApiV3.getUserLinksCommented(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<LinkResponseV3>>(errorBodyParser))
                .flatMap { links ->
                    // Endpoint oddaje ZNALEZISKA z komentarzami uzytkownika w srodku -
                    // lista mieszana: znalezisko, a pod nim komentarz / odpowiedz.
                    rxSingle {
                        links.map { response ->
                            EntryLink(link = response.filterLinkV3(owmContentFilter), entry = null).apply {
                                rows = userLinkCommentRows(response.id, response.comments.items.orEmpty())
                            }
                        }
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
        ): Single<List<EntryLink>> =
            rxSingle { profileApiV3.getUserLinksRelated(username, page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<LinkResponseV3>>(errorBodyParser))
                .map { links ->
                    // Znalezisko, a pod nim powiazane dodane przez uzytkownika.
                    links.map { response ->
                        EntryLink(link = response.filterLinkV3(owmContentFilter), entry = null).apply {
                            rows =
                                response.related
                                    ?.items
                                    .orEmpty()
                                    .map { EntryListRow.RelatedRow(RelatedMapperV3.map(it), linkId = response.id) }
                        }
                    }
                }

        /**
         * Komentarze uzytkownika pod znaleziskiem jako wiersze listy mieszanej. Odpowiedz
         * w watku idzie pod komentarzem, na ktory odpowiada - API podaje tylko jego id,
         * wiec tresc dociagamy (rownolegle, raz na watek). Gdy sie nie uda (np. komentarz
         * usuniety), odpowiedz zostaje sama.
         */
        private suspend fun userLinkCommentRows(
            linkId: Long,
            comments: List<LinkCommentResponseV3>,
        ): List<EntryListRow.LinkCommentRow> =
            coroutineScope {
                val threads = LinkedHashMap<Long, MutableList<LinkCommentResponseV3>>()
                comments.forEach { comment -> threads.getOrPut(comment.parentId ?: comment.id) { mutableListOf() } += comment }
                val ownIds = comments.mapTo(HashSet()) { it.id }
                val fetchedParents =
                    threads.keys
                        .filterNot { it in ownIds }
                        .map { parentId ->
                            async {
                                try {
                                    linksApiV3.getLinkComment(linkId = linkId, commentId = parentId).data
                                } catch (failure: CancellationException) {
                                    throw failure
                                } catch (failure: Exception) {
                                    Napier.w("Nie udalo sie pobrac komentarza-rodzica $parentId", failure)
                                    null
                                }
                            }
                        }.awaitAll()
                        .filterNotNull()
                        .associateBy { it.id }

                fun LinkCommentResponseV3.toRow(isReply: Boolean) =
                    EntryListRow.LinkCommentRow(LinkCommentMapperV3.map(this, owmContentFilter, linkId), isReply)

                threads.flatMap { (threadId, items) ->
                    val parent = items.firstOrNull { it.id == threadId } ?: fetchedParents[threadId]
                    val replies = items.filter { it.id != threadId }.sortedBy { it.createdAt }
                    listOfNotNull(parent?.toRow(isReply = false)) + replies.map { it.toRow(isReply = true) }
                }
            }

        override fun observe(tag: String) =
            rxSingle { profileApiV3.observeUser(tag) }
                .retryWhen(userTokenRefresher)
                .map { ObserveStateResponse(isObserved = true, isBlocked = false) }

        override fun unobserve(tag: String) =
            rxSingle { profileApiV3.unobserveUser(tag) ?: WykopApiResponseV3(data = Unit, pagination = null) }
                .retryWhen(userTokenRefresher)
                .map { ObserveStateResponse(isObserved = false, isBlocked = false) }

        override fun block(tag: String) =
            rxSingle {
                val username = tag.removePrefix("@")
                val response = profileApiV3.blockUser(WykopApiRequestV3(BlacklistUserRequestV3(username = username)))
                if (!response.isSuccessful && response.code() != 409) throw HttpException(response)
            }.retryWhen(userTokenRefresher)
                .map {
                    appStorage.blacklistQueries.insertOrReplaceProfile(tag.removePrefix("@"))
                    ObserveStateResponse(isObserved = false, isBlocked = true)
                }

        override fun unblock(tag: String) =
            rxSingle {
                val response = profileApiV3.unblockUser(tag.removePrefix("@"))
                if (!response.isSuccessful && response.code() != 409) throw HttpException(response)
            }.retryWhen(userTokenRefresher)
                .map {
                    appStorage.blacklistQueries.deleteProfile(tag.removePrefix("@"))
                    ObserveStateResponse(isObserved = false, isBlocked = false)
                }
    }

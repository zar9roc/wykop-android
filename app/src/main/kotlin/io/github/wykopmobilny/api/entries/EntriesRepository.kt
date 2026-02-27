package io.github.wykopmobilny.api.entries

import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.endpoints.EntriesRetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformer
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.patrons.PatronsApi
import io.github.wykopmobilny.api.toRequestBody
import io.github.wykopmobilny.models.dataclass.EntryVotePublishModel
import io.github.wykopmobilny.models.mapper.apiv2.SurveyMapper
import io.github.wykopmobilny.models.mapper.apiv2.VoterMapper
import io.github.wykopmobilny.models.mapper.apiv2.filterEntries
import io.github.wykopmobilny.models.mapper.apiv2.filterEntry
import io.github.wykopmobilny.models.mapper.apiv3.filterEntriesV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntriesRepository
    @Inject
    constructor(
        private val entriesApi: EntriesRetrofitApi,
        private val entriesApiV3: io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
    ) : EntriesApi {
        override val entryVoteSubject = PublishSubject.create<EntryVotePublishModel>()
        override val entryUnVoteSubject = PublishSubject.create<EntryVotePublishModel>()

        override fun voteEntry(entryId: Long) =
            rxSingle { entriesApi.voteEntry(entryId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())
                .doOnSuccess { entryVoteSubject.onNext(EntryVotePublishModel(entryId, it)) }

        override fun unvoteEntry(entryId: Long) =
            rxSingle { entriesApi.unvoteEntry(entryId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())
                .doOnSuccess { entryUnVoteSubject.onNext(EntryVotePublishModel(entryId, it)) }

        override fun voteComment(commentId: Long) =
            rxSingle { entriesApi.voteComment(commentId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun unvoteComment(commentId: Long) =
            rxSingle { entriesApi.unvoteComment(commentId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun addEntry(
            body: String,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            entriesApi.addEntry(
                body = body.allowImageOnly().toRequestBody(),
                plus18 = plus18.toRequestBody(),
                file = wykopImageFile.getFileMultipart(),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun addEntry(
            body: String,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle { entriesApi.addEntry(body, embed, plus18) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun addEntryComment(
            body: String,
            entryId: Long,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            entriesApi.addEntryComment(
                body = body.allowImageOnly().toRequestBody(),
                plus18 = plus18.toRequestBody(),
                entryId = entryId,
                file = wykopImageFile.getFileMultipart(),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun addEntryComment(
            body: String,
            entryId: Long,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle { entriesApi.addEntryComment(body, embed, plus18, entryId) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun editEntry(
            body: String,
            entryId: Long,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle { entriesApi.editEntry(body = body, embed = embed, plus18 = plus18, entryId = entryId) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun editEntry(
            body: String,
            entryId: Long,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            entriesApi.editEntry(
                body = body.toRequestBody(),
                plus18 = plus18.toRequestBody(),
                entryId = entryId,
                file = wykopImageFile.getFileMultipart(),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun markFavorite(entryId: Long) =
            rxSingle { entriesApi.markFavorite(entryId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun deleteEntry(entryId: Long) =
            rxSingle { entriesApi.deleteEntry(entryId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun editEntryComment(
            body: String,
            commentId: Long,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle { entriesApi.editEntryComment(body, embed, plus18, commentId) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun editEntryComment(
            body: String,
            commentId: Long,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            entriesApi.editEntryComment(
                body = body.toRequestBody(),
                plus18 = plus18.toRequestBody(),
                commentId = commentId,
                file = wykopImageFile.getFileMultipart(),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())

        override fun deleteEntryComment(commentId: Long) =
            rxSingle { entriesApi.deleteEntryComment(commentId) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformer())

        override fun voteSurvey(
            entryId: Long,
            answerId: Int,
        ) = rxSingle { entriesApi.voteSurvey(entryId, answerId) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformer())
            .map { SurveyMapper.map(it) }

        override fun getHot(
            page: Int,
            period: String,
        ) = rxSingle { entriesApiV3.getHot(page, "best") }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>>())
            .map { entries ->
                entries.filterEntriesV3(owmContentFilter = owmContentFilter)
            }

        override fun getStream(page: Int) =
            rxSingle { entriesApiV3.getStream(page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>>())
                .map { entries ->
                    entries.filterEntriesV3(owmContentFilter = owmContentFilter)
                }

        override fun getActive(page: Int) =
            rxSingle { entriesApiV3.getActive(page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>>())
                .map { entries ->
                    entries.filterEntriesV3(owmContentFilter = owmContentFilter)
                }

        override fun getObserved(page: Int) =
            rxSingle { entriesApiV3.getObserved(page) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>>())
                .map { entries ->
                    entries.filterEntriesV3(owmContentFilter = owmContentFilter)
                }

        override fun getEntry(id: Long) =
            rxSingle { entriesApiV3.getEntry(id) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>())
                .map { entry ->
                    entry.filterEntryV3(owmContentFilter = owmContentFilter)
                }

        override fun getEntryVoters(id: Long) =
            rxSingle { entriesApiV3.getEntryVoters(id) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3>>())
                .map { users ->
                    users.map { userResponse ->
                        io.github.wykopmobilny.models.mapper.apiv3.VoterMapperV3
                            .map(userResponse)
                    }
                }

        override fun getEntryCommentVoters(id: Long) =
            rxSingle { entriesApiV3.getCommentUpvoters(id) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3>>())
                .map { users ->
                    users.map { userResponse ->
                        io.github.wykopmobilny.models.mapper.apiv3.VoterMapperV3
                            .map(userResponse)
                    }
                }
    }

internal fun String.allowImageOnly() = ifEmpty { " " }

package io.github.wykopmobilny.api.entries

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.exceptions.handleMediaUpload
import io.github.wykopmobilny.api.filters.OWMContentFilter
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.media.UploadPhotoByUrlRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateCommentRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.CreateUpdateEntryRequestV3
import io.github.wykopmobilny.api.requests.v3.entries.VoteSurveyRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import retrofit2.HttpException
import retrofit2.Response
import io.github.wykopmobilny.api.responses.EntryCommentResponse
import io.github.wykopmobilny.api.responses.EntryResponse
import io.github.wykopmobilny.api.toRequestBody
import io.github.wykopmobilny.models.dataclass.EntryVotePublishModel
import io.github.wykopmobilny.models.mapper.apiv2.SurveyMapper
import io.github.wykopmobilny.models.mapper.apiv3.EntryCommentMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntriesV3
import io.github.wykopmobilny.models.mapper.apiv3.filterEntryV3
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.rx2.rxSingle
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntriesRepository
    @Inject
    constructor(
        private val entriesApiV3: io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi,
        private val mediaApiV3: io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi,
        private val favouritesApiV3: io.github.wykopmobilny.api.endpoints.v3.FavouritesV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val owmContentFilter: OWMContentFilter,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : EntriesApi {
        override val entryVoteSubject = PublishSubject.create<EntryVotePublishModel>()
        override val entryUnVoteSubject = PublishSubject.create<EntryVotePublishModel>()

        override fun voteEntry(entryId: Long) =
            rxSingle {
                val response = entriesApiV3.voteEntry(entryId)
                if (!response.isSuccessful) throw HttpException(response)
                WykopApiResponseV3(data = Unit, pagination = null)
            }.retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
                .map {
                    io.github.wykopmobilny.api.responses
                        .VoteResponse(null)
                }.doOnSuccess { entryVoteSubject.onNext(EntryVotePublishModel(entryId, it)) }

        override fun unvoteEntry(entryId: Long) =
            rxSingle {
                val response = entriesApiV3.unvoteEntry(entryId)
                if (!response.isSuccessful) throw HttpException(response)
                WykopApiResponseV3(data = Unit, pagination = null)
            }.retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
                .map {
                    io.github.wykopmobilny.api.responses
                        .VoteResponse(null)
                }.doOnSuccess { entryUnVoteSubject.onNext(EntryVotePublishModel(entryId, it)) }

        override fun voteComment(
            entryId: Long,
            commentId: Long,
        ) = rxSingle {
            val response = entriesApiV3.voteComment(entryId, commentId)
            if (!response.isSuccessful) throw HttpException(response)
            WykopApiResponseV3(data = Unit, pagination = null)
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                io.github.wykopmobilny.api.responses
                    .VoteResponse(null)
            }

        override fun unvoteComment(
            entryId: Long,
            commentId: Long,
        ) = rxSingle {
            val response = entriesApiV3.unvoteComment(entryId, commentId)
            if (!response.isSuccessful) throw HttpException(response)
            WykopApiResponseV3(data = Unit, pagination = null)
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                io.github.wykopmobilny.api.responses
                    .VoteResponse(null)
            }

        override fun addEntry(
            body: String,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoAndGetKey(wykopImageFile)

            entriesApiV3.addEntry(
                WykopApiRequestV3(
                    CreateUpdateEntryRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>(errorBodyParser))
            .map { entryV3 ->
                // Minimal mapping for API compatibility - only ID is used by callers
                EntryResponse(
                    id = entryV3.id,
                    date = Instant.DISTANT_PAST,
                    body = entryV3.content,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    commentsCount = 0,
                    comments = null,
                    status = "",
                    embed = null,
                    survey = null,
                    userVote = 0,
                    violationUrl = null,
                    app = null,
                    isCommentingPossible = null,
                )
            }

        override fun addEntry(
            body: String,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoUrlAndGetKey(embed)
            entriesApiV3.addEntry(
                WykopApiRequestV3(
                    CreateUpdateEntryRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>(errorBodyParser))
            .map { entryV3 ->
                // Minimal mapping for API compatibility - only ID is used by callers
                EntryResponse(
                    id = entryV3.id,
                    date = Instant.DISTANT_PAST,
                    body = entryV3.content,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    commentsCount = 0,
                    comments = null,
                    status = "",
                    embed = null,
                    survey = null,
                    userVote = 0,
                    violationUrl = null,
                    app = null,
                    isCommentingPossible = null,
                )
            }

        override fun addEntryComment(
            body: String,
            entryId: Long,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoAndGetKey(wykopImageFile)

            entriesApiV3.addEntryComment(
                entryId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3>(errorBodyParser))
            .map { commentV3 ->
                // Minimal mapping for API compatibility
                EntryCommentResponse(
                    id = commentV3.id,
                    entryId = entryId,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = commentV3.content,
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun addEntryComment(
            body: String,
            entryId: Long,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoUrlAndGetKey(embed)
            entriesApiV3.addEntryComment(
                entryId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3>(errorBodyParser))
            .map { commentV3 ->
                // Minimal mapping for API compatibility
                EntryCommentResponse(
                    id = commentV3.id,
                    entryId = entryId,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = commentV3.content,
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun editEntry(
            body: String,
            entryId: Long,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoUrlAndGetKey(embed)
            entriesApiV3.editEntry(
                entryId,
                WykopApiRequestV3(
                    CreateUpdateEntryRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                // API v3 returns 200 with no body, return minimal EntryCommentResponse for compatibility
                EntryCommentResponse(
                    id = entryId,
                    entryId = null,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = body,
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun editEntry(
            body: String,
            entryId: Long,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoAndGetKey(wykopImageFile)

            entriesApiV3.editEntry(
                entryId,
                WykopApiRequestV3(
                    CreateUpdateEntryRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                // API v3 returns 200 with no body, return minimal EntryCommentResponse for compatibility
                EntryCommentResponse(
                    id = entryId,
                    entryId = null,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = body,
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun markFavorite(
            entryId: Long,
            currentlyFavorite: Boolean,
        ) = rxSingle {
            val request =
                WykopApiRequestV3(
                    io.github.wykopmobilny.api.requests.v3.favourites.FavouriteRequestV3(
                        type = "entry",
                        sourceId = entryId,
                    ),
                )
            if (currentlyFavorite) {
                favouritesApiV3.removeFavourite(request)
            } else {
                favouritesApiV3.addFavourite(request)
            }
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map { !currentlyFavorite }

        override fun deleteEntry(entryId: Long) =
            rxSingle { entriesApiV3.deleteEntry(entryId).requireSuccessful() }
                .retryWhen(userTokenRefresher)
                .map {
                    // API v3 returns 204 with no body, return minimal EntryResponse for compatibility
                    EntryResponse(
                        id = entryId,
                        date = Instant.DISTANT_PAST,
                        body = "",
                        author =
                            io.github.wykopmobilny.api.responses
                                .AuthorResponse("", 0, null, ""),
                        blocked = false,
                        favorite = false,
                        voteCount = 0,
                        commentsCount = 0,
                        comments = null,
                        status = "",
                        embed = null,
                        survey = null,
                        userVote = 0,
                        violationUrl = null,
                        app = null,
                        isCommentingPossible = null,
                    )
                }

        override fun editEntryComment(
            body: String,
            entryId: Long,
            commentId: Long,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoUrlAndGetKey(embed)
            entriesApiV3.editEntryComment(
                entryId,
                commentId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                // API v3 returns 200 with no body, return minimal EntryCommentResponse for compatibility
                EntryCommentResponse(
                    id = commentId,
                    entryId = entryId,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = body,
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun editEntryComment(
            body: String,
            entryId: Long,
            commentId: Long,
            wykopImageFile: WykopImageFile,
            plus18: Boolean,
        ) = rxSingle {
            val photoKey = uploadPhotoAndGetKey(wykopImageFile)

            entriesApiV3.editEntryComment(
                entryId,
                commentId,
                WykopApiRequestV3(
                    CreateUpdateCommentRequestV3(
                        content = body,
                        photo = photoKey,
                        adult = plus18,
                    ),
                ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                // API v3 returns 200 with no body, return minimal EntryCommentResponse for compatibility
                EntryCommentResponse(
                    id = commentId,
                    entryId = entryId,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = body,
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun deleteEntryComment(
            entryId: Long,
            commentId: Long,
        ) = rxSingle { entriesApiV3.deleteEntryComment(entryId, commentId).requireSuccessful() }
            .retryWhen(userTokenRefresher)
            .map {
                // API v3 returns 204 with no body, return minimal EntryCommentResponse for compatibility
                EntryCommentResponse(
                    id = commentId,
                    entryId = entryId,
                    author =
                        io.github.wykopmobilny.api.responses
                            .AuthorResponse("", 0, null, ""),
                    date = "",
                    body = "",
                    blocked = false,
                    favorite = false,
                    voteCount = 0,
                    status = "",
                    userVote = 0,
                    embed = null,
                    app = null,
                    violationUrl = null,
                )
            }

        override fun voteSurvey(
            entryId: Long,
            answerId: Int,
        ) = rxSingle {
            val response =
                entriesApiV3.voteSurvey(
                    entryId,
                    WykopApiRequestV3(
                        VoteSurveyRequestV3(answerId),
                    ),
                )
            if (!response.isSuccessful) throw HttpException(response)
            WykopApiResponseV3(data = Unit, pagination = null)
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<Unit>(errorBodyParser))
            .map {
                // API v3 returns 201 with no body, return empty Survey for compatibility
                io.github.wykopmobilny.models.dataclass.Survey(
                    question = "",
                    answers = emptyList(),
                    userAnswer = answerId,
                )
            }

        override fun getHot(
            page: String?,
            period: String,
        ) = rxSingle {
            val lastUpdate =
                when (period) {
                    "1" -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.ONE_HOUR
                    "2" -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.TWO_HOURS
                    "3" -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.THREE_HOURS
                    "6" -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.SIX_HOURS
                    "12" -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.TWELVE_HOURS
                    "24" -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.TWENTY_FOUR_HOURS
                    else -> io.github.wykopmobilny.api.endpoints.v3.EntriesLastUpdate.TWELVE_HOURS
                }
            entriesApiV3.getEntries(
                page = page,
                sort = io.github.wykopmobilny.api.endpoints.v3.EntriesSort.HOT,
                lastUpdate = lastUpdate,
            )
        }.retryWhen(userTokenRefresher)
            .map { response ->
                response.data.orEmpty().filterEntriesV3(owmContentFilter, response.pagination)
            }

        override fun getStream(page: String?) =
            rxSingle {
                entriesApiV3.getEntries(
                    page = page,
                    sort = io.github.wykopmobilny.api.endpoints.v3.EntriesSort.NEWEST,
                    lastUpdate = null,
                )
            }.retryWhen(userTokenRefresher)
                .map { response ->
                    response.data.orEmpty().filterEntriesV3(owmContentFilter, response.pagination)
                }

        override fun getActive(page: String?) =
            rxSingle {
                entriesApiV3.getEntries(
                    page = page,
                    sort = io.github.wykopmobilny.api.endpoints.v3.EntriesSort.ACTIVE,
                    lastUpdate = null,
                )
            }.retryWhen(userTokenRefresher)
                .map { response ->
                    response.data.orEmpty().filterEntriesV3(owmContentFilter, response.pagination)
                }

        override fun getObserved(page: String?) =
            rxSingle { favouritesApiV3.getFavourites(page) }
                .retryWhen(userTokenRefresher)
                .map { response ->
                    val entries =
                        response.data
                            .orEmpty()
                            .filterIsInstance<ObservedItemV3.EntryItem>()
                            .map { it.entry }
                    entries.filterEntriesV3(owmContentFilter, response.pagination)
                }

        override fun getEntry(id: Long) =
            rxSingle { entriesApiV3.getEntry(id) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3>(errorBodyParser))
                .map { entry ->
                    entry.filterEntryV3(owmContentFilter = owmContentFilter)
                }

        override fun getEntryComments(id: Long) =
            rxSingle { entriesApiV3.getEntryComments(id) }
                .retryWhen(userTokenRefresher)
                .compose(
                    ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.entries.EntryCommentResponseV3>>(
                        errorBodyParser,
                    ),
                ).map { comments ->
                    comments.map { comment ->
                        EntryCommentMapperV3.map(comment, owmContentFilter, entryId = id)
                    }
                }

        override fun getEntryVoters(id: Long) =
            rxSingle { entriesApiV3.getEntryVoters(id) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3>>(errorBodyParser))
                .map { users ->
                    users.map { userResponse ->
                        io.github.wykopmobilny.models.mapper.apiv3.VoterMapperV3
                            .map(userResponse)
                    }
                }

        override fun getEntryCommentVoters(
            entryId: Long,
            commentId: Long,
        ) = rxSingle { entriesApiV3.getCommentVoters(entryId, commentId) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3>>(errorBodyParser))
            .map { users ->
                users.map { userResponse ->
                    io.github.wykopmobilny.models.mapper.apiv3.VoterMapperV3
                        .map(userResponse)
                }
            }

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

        // Obraz z URL: v3 nie przyjmuje adresu jako "embed" (to pole na embedy wideo) -
        // trzeba wgrac go przez /media/photos i wyslac zwrocony klucz jako "photo".
        private suspend fun uploadPhotoUrlAndGetKey(url: String?): String? {
            val photoUrl = url?.takeIf { it.isNotBlank() } ?: return null
            return handleMediaUpload {
                mediaApiV3.uploadPhotoByUrl(WykopApiRequestV3(UploadPhotoByUrlRequestV3(url = photoUrl)))
            }.key
        }
    }

internal fun String.allowImageOnly() = ifEmpty { " " }

// 204 No Content: ten Retrofit rzuca NPE przy nienullowalnym zwrocie z pustym body
// (ignoruje '?'), a body-owe WykopApiResponseV3 nie parsuje sie z pustej odpowiedzi.
// Response<Unit> pozwala sprawdzic status; 409 = zasob juz w docelowym stanie (np.
// komentarz usuniety rownolegle) - traktujemy jak sukces.
internal fun Response<Unit>.requireSuccessful() {
    if (!isSuccessful && code() != 409) throw HttpException(this)
}

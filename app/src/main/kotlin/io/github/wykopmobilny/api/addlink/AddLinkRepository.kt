package io.github.wykopmobilny.api.addlink

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.errorhandler.WykopExceptionParser
import io.github.wykopmobilny.api.exceptions.handleMediaUpload
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.links.LinkDraftCreateRequestV3
import io.github.wykopmobilny.api.requests.v3.links.LinkDraftPublishRequestV3
import io.github.wykopmobilny.api.requests.v3.media.UploadPhotoByUrlRequestV3
import io.github.wykopmobilny.api.responses.AddLinkPreviewImage
import io.github.wykopmobilny.api.responses.NewLinkInformationResponse
import io.github.wykopmobilny.api.responses.NewLinkResponse
import io.github.wykopmobilny.api.responses.v3.links.LinkDraftCreateResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkDraftResponseV3
import kotlinx.coroutines.rx2.rxSingle
import retrofit2.HttpException
import javax.inject.Inject

class AddLinkRepository
    @Inject
    constructor(
        private val linksV3Api: LinksV3RetrofitApi,
        private val mediaApiV3: MediaV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : AddLinkApi {
        // Krok 1: utworz draft z URL, potem pobierz zescrapowane dane (tytul/opis/obrazki).
        // Gdy `duplicate == true` (dokladnie ten URL juz jest znaleziskiem), API nie utrwala
        // draftu - GET /draft/{key} zwrocilby wtedy 404, wiec od razu zglaszamy przyjazny blad.
        // Blizsze duplikaty (`similar`, gdy duplicate=false) nie blokuja dodania i sa pomijane.
        override fun getDraft(url: String) =
            rxSingle { linksV3Api.createLinkDraft(WykopApiRequestV3(LinkDraftCreateRequestV3(url))) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<LinkDraftCreateResponseV3>(errorBodyParser))
                .flatMap { created ->
                    if (created.duplicate == true) {
                        throw WykopExceptionParser.WykopApiException(
                            code = 409,
                            message = "To znalezisko zostało już dodane wcześniej.",
                        )
                    }
                    rxSingle { linksV3Api.getLinkDraft(created.key) }
                        .retryWhen(userTokenRefresher)
                        .compose(ErrorHandlerTransformerV3<LinkDraftResponseV3>(errorBodyParser))
                }.map { draft ->
                    NewLinkResponse(
                        data =
                            NewLinkInformationResponse(
                                key = draft.key,
                                title = draft.title.orEmpty(),
                                description = draft.description.orEmpty(),
                                sourceUrl = draft.url,
                            ),
                        error = null,
                        duplicates = emptyList(),
                    )
                }

        override fun getImages(key: String) =
            rxSingle { linksV3Api.getLinkDraft(key) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<LinkDraftResponseV3>(errorBodyParser))
                .map { draft ->
                    draft.images.orEmpty().map { image ->
                        // v3 podaje tylko URL obrazka (bez key/type) - uzywamy URL jako identyfikatora.
                        AddLinkPreviewImage(
                            key = image.url,
                            type = "image",
                            previewUrl = image.url,
                            sourceUrl = image.url,
                        )
                    }
                }

        override fun publishLink(
            key: String,
            title: String,
            description: String,
            tags: String,
            photo: String,
            url: String,
            plus18: Boolean,
        ) = rxSingle {
            // Wybrana miniatura to URL scrapowanego obrazka. API oczekuje w polu "photo"
            // KLUCZA media, wiec najpierw wgrywamy URL przez /media/photos (type=links).
            val photoKey =
                photo.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    handleMediaUpload {
                        mediaApiV3.uploadPhotoByUrl(
                            WykopApiRequestV3(UploadPhotoByUrlRequestV3(url = imageUrl)),
                            type = "links",
                        )
                    }.key
                }
            val response =
                linksV3Api.publishLinkDraft(
                    key = key,
                    request =
                        WykopApiRequestV3(
                            LinkDraftPublishRequestV3(
                                title = title,
                                description = description,
                                tags = tags.toTagList(),
                                adult = plus18,
                                photo = photoKey,
                            ),
                        ),
                )
            if (!response.isSuccessful) throw HttpException(response)
        }.retryWhen(userTokenRefresher)

        override fun editLink(
            linkId: Long,
            title: String,
            description: String,
            tags: String,
            plus18: Boolean,
        ) = rxSingle {
            val response =
                linksV3Api.editLink(
                    linkId = linkId,
                    request =
                        WykopApiRequestV3(
                            LinkDraftPublishRequestV3(
                                title = title,
                                description = description,
                                tags = tags.toTagList(),
                                adult = plus18,
                            ),
                        ),
                )
            if (!response.isSuccessful) throw HttpException(response)
        }.retryWhen(userTokenRefresher)
    }

// "#rumunia, #nieruchomosci hakujo" -> ["rumunia","nieruchomosci","hakujo"]
private fun String.toTagList(): List<String> =
    split(Regex("[\\s,]+"))
        .map { it.removePrefix("#").trim() }
        .filter { it.isNotBlank() }

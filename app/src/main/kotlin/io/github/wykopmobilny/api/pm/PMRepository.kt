package io.github.wykopmobilny.api.pm

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.PmV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.exceptions.handleMediaUpload
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.media.UploadPhotoByUrlRequestV3
import io.github.wykopmobilny.api.requests.v3.pm.CreatePmMessageRequestV3
import io.github.wykopmobilny.api.responses.ConversationDeleteResponse
import io.github.wykopmobilny.api.responses.v3.pm.PmConversationMessagesResponseV3
import io.github.wykopmobilny.api.responses.v3.pm.PmConversationResponseV3
import io.github.wykopmobilny.api.responses.v3.pm.PmMessageResponseV3
import io.github.wykopmobilny.kotlin.convertWykopContentToHtml
import io.github.wykopmobilny.models.dataclass.Conversation
import io.github.wykopmobilny.models.dataclass.FullConversation
import io.github.wykopmobilny.models.dataclass.PMMessage
import io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
import io.github.wykopmobilny.models.mapper.apiv3.MediaMapperV3
import io.github.wykopmobilny.utils.toPrettyDate
import kotlinx.coroutines.rx2.rxSingle
import retrofit2.HttpException
import javax.inject.Inject

// PmMessage.type: 0 = wiadomosc zalogowanego uzytkownika, 1 = wiadomosc rozmowcy
private const val MESSAGE_TYPE_SENT = 0

private fun PmMessageResponseV3.toPMMessage() =
    PMMessage(
        date = createdAt?.toPrettyDate().orEmpty(),
        body = content.orEmpty().convertWykopContentToHtml(),
        embed = media?.let(MediaMapperV3::map),
        isSentFromUser = type == MESSAGE_TYPE_SENT,
        app = null,
    )

private fun PmConversationResponseV3.toConversation() =
    Conversation(
        user = AuthorMapperV3.map(user),
        lastUpdate = lastMessage?.createdAt?.toPrettyDate().orEmpty(),
    )

private fun PmConversationMessagesResponseV3.toFullConversation() =
    FullConversation(
        messages = messages.orEmpty().map { it.toPMMessage() },
        receiver = AuthorMapperV3.map(user),
    )

class PMRepository
    @Inject
    constructor(
        private val pmApiV3: PmV3RetrofitApi,
        private val mediaApiV3: MediaV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : PMApi {
        override fun getConversations() =
            rxSingle { pmApiV3.getConversations() }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<PmConversationResponseV3>>(errorBodyParser))
                .map { it.map { response -> response.toConversation() } }

        override fun getConversation(user: String) =
            rxSingle { pmApiV3.getConversation(user) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<PmConversationMessagesResponseV3>(errorBodyParser))
                .map { it.toFullConversation() }

        override fun deleteConversation(user: String) =
            rxSingle {
                val response = pmApiV3.deleteConversation(user)
                if (!response.isSuccessful) throw HttpException(response)
                ConversationDeleteResponse(status = "ok")
            }.retryWhen(userTokenRefresher)

        override fun sendMessage(
            body: String,
            user: String,
            embed: String?,
            plus18: Boolean,
        ) = rxSingle {
            // Obraz z URL: v3 nie przyjmuje adresu jako "embed" - wgrywamy przez
            // /media/photos (type=conversations) i wysylamy klucz w polu "photo".
            val photoKey =
                embed?.takeIf { it.isNotBlank() }?.let {
                    handleMediaUpload {
                        mediaApiV3.uploadPhotoByUrl(
                            WykopApiRequestV3(UploadPhotoByUrlRequestV3(url = it)),
                            type = "conversations",
                        )
                    }.key
                }
            pmApiV3.sendMessage(
                username = user,
                body = WykopApiRequestV3(CreatePmMessageRequestV3(content = body.ifEmpty { " " }, photo = photoKey)),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<PmMessageResponseV3>(errorBodyParser))
            .map { it.toPMMessage() }

        override fun sendMessage(
            body: String,
            user: String,
            plus18: Boolean,
            embed: WykopImageFile,
        ) = rxSingle {
            val photoKey =
                handleMediaUpload {
                    mediaApiV3.uploadPhoto(embed.getFileMultipartForV3())
                }.key
            pmApiV3.sendMessage(
                username = user,
                body = WykopApiRequestV3(CreatePmMessageRequestV3(content = body.ifEmpty { " " }, photo = photoKey)),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<PmMessageResponseV3>(errorBodyParser))
            .map { it.toPMMessage() }
    }

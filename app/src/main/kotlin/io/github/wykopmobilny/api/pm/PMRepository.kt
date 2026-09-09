package io.github.wykopmobilny.api.pm

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.PmV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.exceptions.handleMediaUpload
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.pm.CreatePmMessageRequestV3
import io.github.wykopmobilny.api.resolveAttachments
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
import io.github.wykopmobilny.utils.textview.removeHtml
import io.github.wykopmobilny.utils.toPrettyDate
import kotlinx.coroutines.rx2.rxSingle
import retrofit2.HttpException
import javax.inject.Inject

// PmMessage.type: 0 = wiadomosc zalogowanego uzytkownika, 1 = wiadomosc rozmowcy
private const val MESSAGE_TYPE_SENT = 0

private fun PmMessageResponseV3.toPMMessage() =
    PMMessage(
        key = key,
        date = createdAt?.toPrettyDate().orEmpty(),
        body = content.orEmpty().convertWykopContentToHtml(),
        embed = media?.let { MediaMapperV3.map(it, adult = adult ?: false) },
        isSentFromUser = type == MESSAGE_TYPE_SENT,
        app = null,
        isRead = read ?: false,
    )

private fun PmConversationResponseV3.toConversation() =
    Conversation(
        user = AuthorMapperV3.map(user),
        lastUpdate = lastMessage?.createdAt?.toPrettyDate().orEmpty(),
        unread = unread ?: false,
        // Zajawka: treść v3 to markdown - konwersja do HTML i zdjęcie tagów
        // daje czysty tekst jednoliniowy.
        lastMessagePreview =
            lastMessage
                ?.content
                ?.takeIf { it.isNotBlank() }
                ?.convertWykopContentToHtml()
                ?.removeHtml(),
        online = user.online ?: false,
    )

private fun PmConversationMessagesResponseV3.toFullConversation() =
    FullConversation(
        messages = messages.orEmpty().map { it.toPMMessage() },
        receiver = AuthorMapperV3.map(user),
        online = user.online ?: false,
    )

class PMRepository
    @Inject
    constructor(
        private val pmApiV3: PmV3RetrofitApi,
        private val mediaApiV3: MediaV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : PMApi {
        override fun getConversations(
            page: Int,
            query: String?,
        ) = rxSingle { pmApiV3.getConversations(page = page, query = query) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<List<PmConversationResponseV3>>(errorBodyParser))
            .map { it.map { response -> response.toConversation() } }

        override fun getConversation(
            user: String,
            prevMessage: String?,
            nextMessage: String?,
        ) = rxSingle { pmApiV3.getConversation(user, prevMessage = prevMessage, nextMessage = nextMessage) }
            .retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<PmConversationMessagesResponseV3>(errorBodyParser))
            .map { it.toFullConversation() }

        override fun hasNewerMessages(user: String) =
            rxSingle { pmApiV3.getConversationNewer(user) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<Boolean>(errorBodyParser))

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
            embedUrl: String?,
        ) = rxSingle {
            // "embed" (historyczna nazwa) = URL z inputu obrazka: zdjecie idzie przez
            // /media/photos (type=conversations) jako "photo", link medialny (YouTube itp.)
            // przez /media/embed jako "embed". embedUrl = dedykowany slot na link.
            val media = mediaApiV3.resolveAttachments(photoUrl = embed, embedUrl = embedUrl, type = "conversations")
            pmApiV3.sendMessage(
                username = user,
                body =
                    WykopApiRequestV3(
                        CreatePmMessageRequestV3(content = body.ifEmpty { " " }, photo = media.photoKey, embed = media.embedKey),
                    ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<PmMessageResponseV3>(errorBodyParser))
            .map { it.toPMMessage() }

        override fun sendMessage(
            body: String,
            user: String,
            plus18: Boolean,
            embed: WykopImageFile,
            embedUrl: String?,
        ) = rxSingle {
            val photoKey =
                handleMediaUpload {
                    mediaApiV3.uploadPhoto(embed.getFileMultipartForV3())
                }.key
            val media = mediaApiV3.resolveAttachments(photoKey = photoKey, embedUrl = embedUrl, type = "conversations")
            pmApiV3.sendMessage(
                username = user,
                body =
                    WykopApiRequestV3(
                        CreatePmMessageRequestV3(content = body.ifEmpty { " " }, photo = media.photoKey, embed = media.embedKey),
                    ),
            )
        }.retryWhen(userTokenRefresher)
            .compose(ErrorHandlerTransformerV3<PmMessageResponseV3>(errorBodyParser))
            .map { it.toPMMessage() }
    }

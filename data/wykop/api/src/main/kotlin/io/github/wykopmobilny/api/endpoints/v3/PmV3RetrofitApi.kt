package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.pm.CreatePmMessageRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.pm.PmConversationMessagesResponseV3
import io.github.wykopmobilny.api.responses.v3.pm.PmConversationResponseV3
import io.github.wykopmobilny.api.responses.v3.pm.PmMessageResponseV3
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PmV3RetrofitApi {
    @GET("v3/pm/conversations")
    suspend fun getConversations(): WykopApiResponseV3<List<PmConversationResponseV3>>

    @GET("v3/pm/conversations/{username}")
    suspend fun getConversation(
        @Path("username") username: String,
    ): WykopApiResponseV3<PmConversationMessagesResponseV3>

    @POST("v3/pm/conversations/{username}")
    suspend fun sendMessage(
        @Path("username") username: String,
        @Body body: WykopApiRequestV3<CreatePmMessageRequestV3>,
    ): WykopApiResponseV3<PmMessageResponseV3>

    @DELETE("v3/pm/conversations/{username}")
    suspend fun deleteConversation(
        @Path("username") username: String,
    ): Response<Unit>
}

package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MediaV3RetrofitApi {
    @Multipart
    @POST("v3/media/photos/upload")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Query("type") type: String = "comments",
    ): WykopApiResponseV3<PhotoResponseV3>
}

package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationEntryResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationObservedDiscussionResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationPmResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationStatusResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationTagResponseV3
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationsV3RetrofitApi {
    @GET("v3/notifications/status")
    suspend fun getNotificationStatus(): WykopApiResponseV3<NotificationStatusResponseV3>

    @GET("v3/notifications/entries")
    suspend fun getEntryNotifications(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<NotificationEntryResponseV3>>

    @PUT("v3/notifications/entries/all")
    suspend fun markAllEntryNotificationsAsRead(): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/entries/all")
    suspend fun deleteAllEntryNotifications(): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/entries/{id}")
    suspend fun getEntryNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<NotificationEntryResponseV3>

    @PUT("v3/notifications/entries/{id}")
    suspend fun markEntryNotificationAsRead(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/entries/{id}")
    suspend fun deleteEntryNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/pm")
    suspend fun getPmNotifications(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<NotificationPmResponseV3>>

    @PUT("v3/notifications/pm/all")
    suspend fun markAllPmNotificationsAsRead(): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/pm/all")
    suspend fun deleteAllPmNotifications(): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/pm/{id}")
    suspend fun getPmNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<NotificationPmResponseV3>

    @PUT("v3/notifications/pm/{id}")
    suspend fun markPmNotificationAsRead(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/pm/{id}")
    suspend fun deletePmNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/tags")
    suspend fun getTagNotifications(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<NotificationTagResponseV3>>

    @PUT("v3/notifications/tags/all")
    suspend fun markAllTagNotificationsAsRead(): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/tags/all")
    suspend fun deleteAllTagNotifications(): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/tags/{id}")
    suspend fun getTagNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<NotificationTagResponseV3>

    @PUT("v3/notifications/tags/{id}")
    suspend fun markTagNotificationAsRead(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/tags/{id}")
    suspend fun deleteTagNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/observed-discussions")
    suspend fun getObservedDiscussionNotifications(
        @Query("page") page: Int,
    ): WykopApiResponseV3<List<NotificationObservedDiscussionResponseV3>>

    @PUT("v3/notifications/observed-discussions/all")
    suspend fun markAllObservedDiscussionNotificationsAsRead(): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/observed-discussions/all")
    suspend fun deleteAllObservedDiscussionNotifications(): WykopApiResponseV3<Unit>?

    @GET("v3/notifications/observed-discussions/{id}")
    suspend fun getObservedDiscussionNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<NotificationObservedDiscussionResponseV3>

    @PUT("v3/notifications/observed-discussions/{id}")
    suspend fun markObservedDiscussionNotificationAsRead(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>

    @DELETE("v3/notifications/observed-discussions/{id}")
    suspend fun deleteObservedDiscussionNotification(
        @Path("id") id: String,
    ): WykopApiResponseV3<Unit>?
}

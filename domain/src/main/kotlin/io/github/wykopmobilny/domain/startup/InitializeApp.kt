package io.github.wykopmobilny.domain.startup

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.auth.AuthRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.domain.settings.prefs.GetNotificationPreferences
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import io.github.wykopmobilny.work.WorkScheduler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class InitializeApp
    @Inject
    internal constructor(
        private val workScheduler: WorkScheduler,
        private val appConfig: AppConfig,
        private val getNotificationPreferences: GetNotificationPreferences,
        private val authV3Api: AuthV3RetrofitApi,
        private val bearerTokenStorage: BearerTokenStorage,
    ) {
        suspend operator fun invoke() {
            coroutineScope {
                launch { authenticateApp() }
                launch {
                    workScheduler.setupBlacklistRefresh(
                        repeatInterval = appConfig.blacklistRefreshInterval,
                        flexDuration = appConfig.blacklistFlexInterval,
                    )
                }
                launch {
                    getNotificationPreferences()
                        .map { it.notificationsEnabled to it.notificationRefreshPeriod }
                        .distinctUntilChanged()
                        .collect { (enabled, refreshPeriod) ->
                            workScheduler.cancelNotificationsCheck()
                            if (enabled) {
                                workScheduler.setupNotificationsCheck(
                                    repeatInterval = refreshPeriod.duration,
                                )
                            }
                        }
                }
            }
        }

        private suspend fun authenticateApp() {
            try {
                val response =
                    authV3Api.authenticate(
                        WykopApiRequestV3(
                            data = AuthRequestV3(key = appConfig.v3ApiKey, secret = appConfig.v3ApiSecret),
                        ),
                    )
                val token = response.data?.token
                if (token != null) {
                    bearerTokenStorage.updateBearerToken(token)
                } else {
                    Napier.w("App auth response missing token: ${response.error?.messagePl}")
                }
            } catch (e: HttpException) {
                Napier.w("App auth failed with HTTP ${e.code()}", e)
            } catch (e: IOException) {
                Napier.w("App auth failed due to network error", e)
            }
        }
    }

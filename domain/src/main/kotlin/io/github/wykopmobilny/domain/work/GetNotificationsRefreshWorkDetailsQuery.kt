package io.github.wykopmobilny.domain.work

import io.github.aakira.napier.Napier
import io.github.wykopmobilny.work.GetNotificationsRefreshWorkDetails
import io.github.wykopmobilny.work.WorkData
import javax.inject.Inject

// Worker 15-min: pelny przebieg wszystkich kanalow (wspolna logika w RefreshNotificationsUseCase;
// tryb urgent co ~1 min obsluguje NotificationsPollingService przez NotificationDependencies).
internal class GetNotificationsRefreshWorkDetailsQuery
    @Inject
    constructor(
        private val refreshNotifications: RefreshNotificationsUseCase,
    ) : GetNotificationsRefreshWorkDetails {
        override fun invoke() =
            WorkData(
                onWorkRequested = {
                    runCatching { refreshNotifications(statusFirst = false) }
                        .onSuccess { Napier.i("notification refresh succeeded") }
                        .onFailure { Napier.w("notification refresh failed", it) }
                },
            )
    }

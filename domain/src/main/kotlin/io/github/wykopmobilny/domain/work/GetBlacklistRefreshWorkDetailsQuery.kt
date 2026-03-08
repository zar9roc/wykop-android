package io.github.wykopmobilny.domain.work

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.storage.api.Blacklist
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.work.GetBlacklistRefreshWorkDetails
import io.github.wykopmobilny.work.WorkData
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GetBlacklistRefreshWorkDetailsQuery
    @Inject
    constructor(
        private val jwtTokenStorage: JwtTokenStorage,
        private val store: Store<Unit, Blacklist>,
    ) : GetBlacklistRefreshWorkDetails {
        override fun invoke() =
            run {
                WorkData(
                    onWorkRequested = {
                        if (jwtTokenStorage.jwtToken.first() != null) {
                            runCatching {
                                store.fresh(Unit)
                                Unit
                            }
                        } else {
                            Napier.i("User not logged in, skipping blacklist refresh")
                            Result.success(Unit)
                        }
                    },
                )
            }
    }

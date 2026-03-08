package io.github.wykopmobilny.domain.profile

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import io.github.wykopmobilny.data.cache.api.ProfileDetailsView
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.utils.withResource
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import javax.inject.Inject

internal class InitializeProfile
    @Inject
    constructor(
        private val profileStore: Store<Unit, ProfileDetailsView>,
        private val viewStateStorage: SimpleViewStateStorage,
    ) : ScopeInitializer {
        override suspend fun initialize() {
            withResource(
                refresh = { profileStore.fresh(Unit) },
                update = { resource -> viewStateStorage.update { resource } },
            )
        }
    }

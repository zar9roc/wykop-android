package io.github.wykopmobilny.domain.entrydetails

import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsKey
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.domain.utils.withResource
import io.github.wykopmobilny.kotlin.AppScopes
import javax.inject.Inject

internal class InitializeEntryDetails
    @Inject
    constructor(
        private val key: EntryDetailsKey,
        private val pager: EntryCommentsPager,
        private val storage: EntryDetailsStateStorage,
        private val appScopes: AppScopes,
    ) : ScopeInitializer {
        override suspend fun initialize() {
            withResource(
                refresh = { pager.initialLoad() },
                update = { resource -> storage.update { it.copy(generalResource = resource) } },
                launch = { callback -> appScopes.safeKeyed<EntryDetailsScope>(key, block = callback) },
            )
        }
    }

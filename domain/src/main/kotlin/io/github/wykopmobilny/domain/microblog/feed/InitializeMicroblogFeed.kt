package io.github.wykopmobilny.domain.microblog.feed

import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedKey
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.domain.utils.withResource
import io.github.wykopmobilny.kotlin.AppScopes
import javax.inject.Inject

internal class InitializeMicroblogFeed
    @Inject
    constructor(
        private val key: MicroblogFeedKey,
        private val pager: EntryFeedPager,
        private val storage: MicroblogFeedStateStorage,
        private val appScopes: AppScopes,
    ) : ScopeInitializer {
        override suspend fun initialize() {
            withResource(
                refresh = { pager.initialLoad() },
                update = { resource -> storage.update { it.copy(generalResource = resource) } },
                launch = { callback -> appScopes.safeKeyed<MicroblogFeedScope>(key, block = callback) },
            )
        }
    }

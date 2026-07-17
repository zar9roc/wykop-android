package io.github.wykopmobilny.domain.microblog.feed.di

import dagger.Binds
import dagger.Module
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.microblog.feed.GetMicroblogFeedQuery
import io.github.wykopmobilny.domain.microblog.feed.InitializeMicroblogFeed
import io.github.wykopmobilny.entries.feed.GetMicroblogFeed

@Module
internal abstract class MicroblogFeedModule {
    @Binds
    abstract fun getMicroblogFeed(impl: GetMicroblogFeedQuery): GetMicroblogFeed

    @Binds
    abstract fun scopeInitializer(impl: InitializeMicroblogFeed): ScopeInitializer
}

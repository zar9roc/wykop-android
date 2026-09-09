package io.github.wykopmobilny.ui.modules.profile.microblog.voted

import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.profile.ProfileApi
import io.github.wykopmobilny.base.Schedulers
import io.github.wykopmobilny.ui.fragments.entries.EntriesInteractor
import io.github.wykopmobilny.ui.modules.profile.microblog.entries.MicroblogEntriesPresenter
import io.github.wykopmobilny.ui.modules.profile.microblog.entries.MicroblogEntriesSource

@Module
class MicroblogVotedEntriesModule {
    @Provides
    fun providePresenter(
        schedulers: Schedulers,
        profileApi: ProfileApi,
        entriesApi: EntriesApi,
        entriesInteractor: EntriesInteractor,
    ) = MicroblogEntriesPresenter(schedulers, profileApi, entriesApi, entriesInteractor, MicroblogEntriesSource.VOTED)
}

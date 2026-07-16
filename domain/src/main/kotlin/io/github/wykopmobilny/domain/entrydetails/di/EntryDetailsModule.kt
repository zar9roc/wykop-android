package io.github.wykopmobilny.domain.entrydetails.di

import dagger.Binds
import dagger.Module
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.entrydetails.GetEntryDetailsQuery
import io.github.wykopmobilny.domain.entrydetails.InitializeEntryDetails
import io.github.wykopmobilny.entries.details.GetEntryDetails

@Module
internal abstract class EntryDetailsModule {
    @Binds
    abstract fun getEntryDetails(impl: GetEntryDetailsQuery): GetEntryDetails

    @Binds
    abstract fun scopeInitializer(impl: InitializeEntryDetails): ScopeInitializer
}

package io.github.wykopmobilny.domain.microblog.feed.di

import dagger.BindsInstance
import dagger.Subcomponent
import io.github.wykopmobilny.domain.di.HasScopeInitializer
import io.github.wykopmobilny.entries.feed.GetMicroblogFeed
import io.github.wykopmobilny.entries.feed.MicroblogFeedSort

/**
 * Klucz scope'u feedu. `source` rozróżnia feedy (na razie tylko "hot"), `initialSort`
 * to tryb startowy z preferencji - kolejne zmiany trybu idą przez selectSortAction
 * (ten sam scope), więc initialSort jest tylko punktem wejścia.
 */
data class MicroblogFeedKey(
    val source: String,
    val initialSort: MicroblogFeedSort,
)

@MicroblogFeedScope
@Subcomponent(modules = [MicroblogFeedModule::class])
interface MicroblogFeedComponent : HasScopeInitializer {
    fun getMicroblogFeed(): GetMicroblogFeed

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance key: MicroblogFeedKey,
        ): MicroblogFeedComponent
    }
}

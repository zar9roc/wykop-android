package io.github.wykopmobilny.domain.entrydetails.di

import dagger.BindsInstance
import dagger.Subcomponent
import io.github.wykopmobilny.domain.di.HasScopeInitializer
import io.github.wykopmobilny.domain.entrydetails.EntryCommentsPager
import io.github.wykopmobilny.entries.details.GetEntryDetails

/**
 * Klucz scope'u ekranu wpisu (V2). initialPage pochodzi z deep-linka
 * (wykop.pl/wpis/{id}/slug/strona/NNN) - ekran startuje od tej strony
 * komentarzy zamiast ładować wszystkie strony po kolei.
 */
data class EntryDetailsKey(
    val entryId: Long,
    val initialCommentId: Long?,
    val initialPage: Int?,
)

@EntryDetailsScope
@Subcomponent(modules = [EntryDetailsModule::class])
interface EntryDetailsComponent : HasScopeInitializer {
    fun getEntryDetails(): GetEntryDetails

    fun pager(): EntryCommentsPager

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance key: EntryDetailsKey,
        ): EntryDetailsComponent
    }
}

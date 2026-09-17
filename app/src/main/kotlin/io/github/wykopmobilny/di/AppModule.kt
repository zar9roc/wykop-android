package io.github.wykopmobilny.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.utils.usermanager.GuestSessionRefresher

@Module
internal abstract class AppModule {
    @Binds
    abstract fun provideContext(impl: WykopApp): Context

    @Binds
    abstract fun appScopes(impl: WykopApp): AppScopes

    companion object {
        // Most do domeny: graf aplikacji nie widzi DomainComponent bezposrednio,
        // ale ma instancje WykopApp, ktora go trzyma.
        @Provides
        fun guestSessionRefresher(app: WykopApp): GuestSessionRefresher = GuestSessionRefresher { app.refreshGuestSession() }
    }
}

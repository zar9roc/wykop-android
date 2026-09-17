package io.github.wykopmobilny.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.TestApp
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.utils.usermanager.GuestSessionRefresher

@Module
internal abstract class TestAppModule {
    @Binds
    abstract fun provideContext(impl: TestApp): Context

    @Binds
    abstract fun appScopes(impl: TestApp): AppScopes

    // Produkcyjny graf dostaje WykopApp przez @BindsInstance; w testowym wiazemy
    // TestApp jako WykopApp dla klas wstrzykujacych aplikacje wprost
    // (np. NotificationCollapseStorage).
    @Binds
    abstract fun wykopApp(impl: TestApp): WykopApp

    companion object {
        // Lustro providera z AppModule - graf testowy wiaze TestApp zamiast WykopApp.
        @Provides
        fun guestSessionRefresher(app: TestApp): GuestSessionRefresher = GuestSessionRefresher { app.refreshGuestSession() }
    }
}

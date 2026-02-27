package io.github.wykopmobilny.di

import android.content.Context
import dagger.Binds
import dagger.Module
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.kotlin.AppScopes

@Module
internal abstract class AppModule {
    @Binds
    abstract fun provideContext(impl: WykopApp): Context

    @Binds
    abstract fun appScopes(impl: WykopApp): AppScopes
}

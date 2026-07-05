package io.github.wykopmobilny.domain.login.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.domain.login.LoginV3Query
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import io.github.wykopmobilny.ui.login.LoginV3

@Module
internal abstract class LoginModule {
    @Binds
    abstract fun loginV3(impl: LoginV3Query): LoginV3

    companion object {
        @Provides
        @LoginScope
        fun viewState() = SimpleViewStateStorage()
    }
}

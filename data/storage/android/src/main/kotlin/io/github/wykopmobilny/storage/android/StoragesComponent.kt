package io.github.wykopmobilny.storage.android

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.data.storage.api.ReadNotificationEntity
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.storage.api.SessionStorage
import io.github.wykopmobilny.storage.api.Storages
import io.github.wykopmobilny.storage.api.UserInfoStorage
import java.util.concurrent.Executor
import javax.inject.Singleton

@Singleton
@Component(modules = [StoragesModule::class])
interface StoragesComponent : Storages {
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance dbName: String?,
            @BindsInstance context: Context,
            @BindsInstance executor: Executor,
        ): StoragesComponent
    }
}

@Module
internal abstract class StoragesModule {
    companion object {
        @Singleton
        @Provides
        fun database(
            context: Context,
            name: String?,
        ) = AppStorage(
            driver =
                AndroidSqliteDriver(
                    schema = AppStorage.Schema,
                    context = context,
                    name = name,
                ),
            readNotificationEntityAdapter =
                ReadNotificationEntity.Adapter(
                    dismissedAtAdapter = InstantAdapter,
                ),
        )
    }

    @Binds
    abstract fun sessionStorage(impl: CredentialsPreferences): SessionStorage

    @Binds
    abstract fun userInfoStorage(impl: CredentialsPreferences): UserInfoStorage

    @Binds
    abstract fun jwtTokenStorage(impl: CredentialsPreferences): JwtTokenStorage

    @Binds
    @Singleton
    abstract fun bearerTokenStorage(impl: InMemoryBearerTokenStorage): BearerTokenStorage
}

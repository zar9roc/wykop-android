package io.github.wykopmobilny.notification.di

import android.content.Context
import android.content.Intent
import dagger.BindsInstance
import dagger.Component
import io.github.wykopmobilny.notification.NotificationDependencies
import io.github.wykopmobilny.notification.NotificationsApi

@Component(modules = [NotificationsModule::class])
interface NotificationsComponent : NotificationsApi {
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
            @BindsInstance dependencies: NotificationDependencies,
            // interopUrl (null = lista powiadomien) -> Intent
            @BindsInstance interopIntentHandler: @JvmSuppressWildcards (String?) -> Intent,
        ): NotificationsComponent
    }
}

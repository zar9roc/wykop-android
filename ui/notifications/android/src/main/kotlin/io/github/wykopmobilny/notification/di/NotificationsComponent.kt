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
            // interopUrl (null = lista powiadomien) -> back-stack intentow: pierwszy = ekran
            // glowny (dno taska), ostatni = cel. Dzieki temu "wstecz" z ekranu otwartego
            // z powiadomienia wraca do ekranu glownego zamiast zamykac aplikacje.
            @BindsInstance interopIntentHandler: @JvmSuppressWildcards (String?) -> List<Intent>,
        ): NotificationsComponent
    }
}

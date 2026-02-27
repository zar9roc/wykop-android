package io.github.wykopmobilny.domain.navigation.android

import android.app.Application
import android.content.Context
import dagger.Binds
import dagger.Module
import io.github.wykopmobilny.domain.navigation.AppRestarter
import io.github.wykopmobilny.domain.navigation.ClipboardService
import io.github.wykopmobilny.domain.navigation.WykopTextUtils
import io.github.wykopmobilny.domain.navigation.SystemSettingsDetector
import io.github.wykopmobilny.domain.navigation.AppGateway

@Module
internal abstract class FrameworkModule {

    @Binds
    abstract fun appRestarter(impl: AndroidAppRestarter): AppRestarter

    @Binds
    abstract fun context(impl: Application): Context

    @Binds
    abstract fun nightModeDetector(impl: AndroidSystemSettingsDetector): SystemSettingsDetector

    @Binds
    abstract fun youtubeDetector(impl: AndroidAppGateway): AppGateway

    @Binds
    abstract fun htmlUtils(impl: AndroidWykopTextUtils): WykopTextUtils

    @Binds
    abstract fun clipboardService(impl: AndroidClipboardService): ClipboardService
}

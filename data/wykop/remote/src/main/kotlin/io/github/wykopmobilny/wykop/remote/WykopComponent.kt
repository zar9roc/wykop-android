package io.github.wykopmobilny.wykop.remote

import dagger.BindsInstance
import dagger.Component
import io.github.wykopmobilny.api.WykopApi
import io.github.wykopmobilny.storage.api.BearerTokenStorage
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File

@Component(modules = [WykopModule::class])
interface WykopComponent : WykopApi {
    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance okHttpClient: OkHttpClient,
            @BindsInstance @BaseUrl
            baseUrl: String,
            @BindsInstance @AppKey
            appKey: () -> String,
            @BindsInstance @SigningInterceptor
            signingInterceptor: Interceptor,
            @BindsInstance jwtTokenStorage: JwtTokenStorage,
            @BindsInstance bearerTokenStorage: BearerTokenStorage,
            @BindsInstance cacheDir: File,
        ): WykopComponent
    }
}

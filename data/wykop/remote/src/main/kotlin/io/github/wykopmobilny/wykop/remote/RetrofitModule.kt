package io.github.wykopmobilny.wykop.remote

import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.Reusable
import io.github.wykopmobilny.api.ErrorBodyParser
import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.responses.v3.adapters.ObservedItemV3Adapter
import io.github.wykopmobilny.api.responses.v3.adapters.PhpArrayAdapterFactory
import io.github.wykopmobilny.api.responses.v3.adapters.UnitJsonAdapter
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

@Module
internal class RetrofitModule {
    @Provides
    @PathFixingInterceptor
    fun pathFixingInterceptor(impl: AppKeyReplacingInterceptor): Interceptor = impl

    @Provides
    @Reusable
    fun moshi() =
        Moshi
            .Builder()
            .apply {
                add(UnitJsonAdapter.FACTORY)
                add(PhpArrayAdapterFactory())
                add(InstantAdapter())
                add(ObservedItemV3Adapter.FACTORY)
            }.build()

    @Reusable
    @Provides
    fun retrofit(
        okHttpClient: OkHttpClient,
        @PathFixingInterceptor pathFixing: Interceptor,
        @SigningInterceptor signing: Interceptor,
        bearerAuthInterceptor: BearerAuthInterceptor,
        jwtAuthInterceptor: JwtAuthInterceptor,
        forbidden403RetryInterceptor: Forbidden403RetryInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
        @BaseUrl apiUrl: String,
        cacheDir: File,
        moshi: Moshi,
        @IsDebug isDebug: Boolean,
        @DebugNetworkInterceptor debugNetworkInterceptor: Interceptor?,
    ) = Retrofit
        .Builder()
        .client(
            okHttpClient
                .newBuilder()
                .cache(Cache(cacheDir, maxSize = CACHE_SIZE))
                .addInterceptor(pathFixing)
                .addInterceptor(bearerAuthInterceptor)
                .addInterceptor(jwtAuthInterceptor)
                .addInterceptor(forbidden403RetryInterceptor)
                .addInterceptor(signing)
                .apply {
                    if (isDebug) {
                        debugNetworkInterceptor?.let { addInterceptor(it) }
                        addInterceptor(
                            HttpLoggingInterceptor().apply {
                                level = HttpLoggingInterceptor.Level.BODY
                            },
                        )
                    }
                }.authenticator(tokenRefreshAuthenticator)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build(),
        ).baseUrl(apiUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    fun errorBodyParser(errorBodyParser: MoshiErrorBodyParser): ErrorBodyParser = errorBodyParser

    @Provides
    fun errorBodyParserV3(errorBodyParser: MoshiErrorBodyParserV3): ErrorBodyParserV3 = errorBodyParser

    companion object {
        private const val CACHE_SIZE = 10 * 1024 * 1024L
        private const val DEFAULT_TIMEOUT = 30L
    }
}

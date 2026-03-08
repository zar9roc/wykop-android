package io.github.wykopmobilny.debug

import com.squareup.moshi.Moshi
import okhttp3.Interceptor

/**
 * Stub implementation for release builds.
 * MoshiResponseLoggingInterceptor is only active in debug builds.
 */
class MoshiResponseLoggingInterceptor(
    private val moshi: Moshi,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain) = chain.proceed(chain.request())
}

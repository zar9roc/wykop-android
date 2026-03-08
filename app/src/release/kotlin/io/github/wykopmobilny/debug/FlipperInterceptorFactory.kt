package io.github.wykopmobilny.debug

import okhttp3.Interceptor

/**
 * Stub for release builds.
 * Flipper is only available in debug builds.
 */
object FlipperInterceptorFactory {
    fun create(): Interceptor? = null
}

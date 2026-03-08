package io.github.wykopmobilny.debug

import com.facebook.flipper.plugins.network.FlipperOkhttpInterceptor
import okhttp3.Interceptor

/**
 * Factory for creating Flipper OkHttp interceptor.
 * Only available in debug builds.
 */
object FlipperInterceptorFactory {
    /**
     * Creates FlipperOkhttpInterceptor if NetworkFlipperPlugin is available.
     * Returns null if Flipper is not initialized yet.
     */
    fun create(): Interceptor? =
        FlipperPluginHolder.networkPlugin?.let { plugin ->
            FlipperOkhttpInterceptor(plugin)
        }
}

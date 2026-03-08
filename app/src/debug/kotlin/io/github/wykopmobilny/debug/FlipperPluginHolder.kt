package io.github.wykopmobilny.debug

import com.facebook.flipper.plugins.network.NetworkFlipperPlugin

/**
 * Holder for Flipper plugins that need to be accessed from other modules.
 * Only available in debug builds.
 */
object FlipperPluginHolder {
    /**
     * Network plugin for OkHttp interceptor integration.
     * Set by FlipperInitializer and used by RetrofitModule.
     */
    var networkPlugin: NetworkFlipperPlugin? = null
}

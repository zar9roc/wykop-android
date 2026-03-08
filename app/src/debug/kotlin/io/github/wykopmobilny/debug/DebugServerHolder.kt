package io.github.wykopmobilny.debug

import android.content.Context
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi

/**
 * Singleton holder for [DebugHttpServer] to prevent garbage collection.
 *
 * Problem: When DebugHttpServer is created as a local variable in DebugToolsInitializer,
 * it has no strong reference after initialization completes. This allows the GC to collect
 * the server instance, which closes the underlying NanoHTTPD socket, causing "connection refused"
 * errors.
 *
 * Solution: This singleton maintains a strong reference to the server instance for the lifetime
 * of the application process.
 *
 * Usage:
 * ```
 * // Initialize once during app startup (DebugToolsInitializer)
 * DebugServerHolder.initialize(context, entriesApi)
 *
 * // Check if running
 * if (DebugServerHolder.isRunning()) { ... }
 *
 * // Access server (for testing/debugging)
 * val server = DebugServerHolder.getServer()
 * ```
 */
object DebugServerHolder {
    private const val TAG = "DebugServerHolder"

    @Volatile
    private var server: DebugHttpServer? = null

    /**
     * Initialize and start the debug HTTP server.
     * Safe to call multiple times (idempotent).
     */
    fun initialize(context: Context, entriesApi: EntriesV3RetrofitApi) {
        synchronized(this) {
            if (server != null) {
                Napier.d("DebugHttpServer already initialized, skipping", tag = TAG)
                return
            }

            try {
                server = DebugHttpServer(context.applicationContext, entriesApi).apply {
                    start()
                }
                Napier.i(
                    "DebugHttpServer initialized and started on port ${DebugHttpServer.PORT}",
                    tag = TAG,
                )
            } catch (e: Exception) {
                Napier.e("Failed to initialize DebugHttpServer", e, tag = TAG)
                server = null
                throw e
            }
        }
    }

    /**
     * Get the server instance. Returns null if not initialized.
     */
    fun getServer(): DebugHttpServer? = server

    /**
     * Check if the server is running.
     * Note: This checks if the server reference exists and if NanoHTTPD's worker thread is alive.
     */
    fun isRunning(): Boolean = server?.isRunning() == true

    /**
     * Stop the server and clear the reference.
     * Primarily for testing or explicit cleanup.
     */
    fun shutdown() {
        synchronized(this) {
            server?.let {
                try {
                    it.stop()
                    Napier.i("DebugHttpServer stopped", tag = TAG)
                } catch (e: Exception) {
                    Napier.w("Error stopping DebugHttpServer", e, tag = TAG)
                } finally {
                    server = null
                }
            }
        }
    }
}

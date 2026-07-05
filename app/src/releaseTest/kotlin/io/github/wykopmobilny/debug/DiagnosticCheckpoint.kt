package io.github.wykopmobilny.debug

/**
 * Stub implementation for release builds.
 * DiagnosticCheckpoint is only active in debug builds.
 */
object DiagnosticCheckpoint {
    /**
     * No-op stub for release builds.
     */
    fun log(
        @Suppress("UNUSED_PARAMETER") tag: String,
        @Suppress("UNUSED_PARAMETER") message: String,
    ) {
        // No-op in release builds
    }
}

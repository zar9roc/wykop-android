package io.github.wykopmobilny.update

import io.github.wykopmobilny.update.UpdateCheckWorker.Companion.isNewerRelease
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCheckWorkerTest {
    @Test
    fun `newer release is detected`() {
        assertTrue(isNewerRelease("zar-v1.5.2", "1.5.1"))
        assertTrue(isNewerRelease("zar-v1.6.0", "1.5.9"))
        assertTrue(isNewerRelease("zar-v2.0.0", "1.10.3"))
        assertTrue(isNewerRelease("zar-v1.10.0", "1.9.0"))
    }

    @Test
    fun `same or older release is ignored`() {
        assertFalse(isNewerRelease("zar-v1.5.1", "1.5.1"))
        assertFalse(isNewerRelease("zar-v1.5.0", "1.5.1"))
        assertFalse(isNewerRelease("zar-v1.4.9", "1.5.0-SNAPSHOT"))
    }

    @Test
    fun `malformed versions never trigger update`() {
        assertFalse(isNewerRelease("latest", "1.5.1"))
        assertFalse(isNewerRelease("zar-v1.5.2", "dev"))
    }
}

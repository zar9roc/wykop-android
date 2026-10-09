package io.github.wykopmobilny.ui.modules.photoview

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SavedFileNameTest {
    private val url = "https://wykop.pl/cdn/c3201142/abc123def.jpg?author=x&auth=y"

    @Test
    fun `label becomes file name with extension from url`() {
        assertEquals("wakacje.jpg", savedFileName(url, "wakacje"))
    }

    @Test
    fun `label already ending with extension is not doubled`() {
        assertEquals("wakacje.JPG", savedFileName(url, "wakacje.JPG"))
    }

    @Test
    fun `forbidden characters are replaced`() {
        assertEquals("a_b_c.jpg", savedFileName(url, "a/b:c"))
    }

    @Test
    fun `no label falls back to cdn name without query`() {
        assertEquals("abc123def.jpg", savedFileName(url, null))
        assertEquals("abc123def.jpg", savedFileName(url, "  "))
    }

    @Test
    fun `duplicates get index`() {
        val taken = setOf("screenshot.png", "screenshot (2).png")
        assertEquals("screenshot (3).png", uniqueFileName("screenshot.png", taken::contains))
        assertEquals("inny.png", uniqueFileName("inny.png", taken::contains))
        assertEquals("bez (2)", uniqueFileName("bez", setOf("bez")::contains))
    }
}

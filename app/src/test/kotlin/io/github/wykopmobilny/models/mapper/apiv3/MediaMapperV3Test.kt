package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.media.EmbedResponseV3
import io.github.wykopmobilny.api.responses.v3.media.MediaResponseV3
import io.github.wykopmobilny.api.responses.v3.media.PhotoResponseV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaMapperV3Test {
    private fun photo(name: String) =
        PhotoResponseV3(
            url = "https://wykop.pl/cdn/$name.jpg",
            key = name,
            label = null,
            mimeType = "image/jpeg",
            source = null,
            width = 800,
            height = 600,
            size = 1000,
        )

    private val youtube = EmbedResponseV3(type = "youtube", url = "https://youtube.com/watch?v=x", key = "e", thumbnail = null)

    @Test
    fun `gallery photos come first and embed last`() {
        val media = MediaResponseV3(photo = null, photos = listOf(photo("a"), photo("b"), photo("c")), embed = youtube, survey = null)

        val attachments = MediaMapperV3.mapAttachments(media, adult = true)

        assertEquals(listOf("image", "image", "image", "video"), attachments.map { it.type })
        assertEquals("https://wykop.pl/cdn/a.jpg", attachments.first().url)
        assertTrue(attachments.all { it.plus18 })
    }

    @Test
    fun `single photo field is used when there is no gallery`() {
        val media = MediaResponseV3(photo = photo("a"), photos = emptyList(), embed = null, survey = null)

        assertEquals(1, MediaMapperV3.mapAttachments(media, adult = false).size)
    }

    @Test
    fun `gallery wins over duplicated single photo`() {
        val media = MediaResponseV3(photo = photo("a"), photos = listOf(photo("a"), photo("b")), embed = null, survey = null)

        assertEquals(2, MediaMapperV3.mapAttachments(media, adult = false).size)
    }
}

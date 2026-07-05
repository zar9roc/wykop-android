package io.github.wykopmobilny.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WykopContentFormattingTest {
    @Test
    fun `mention with hyphen is linkified as whole login`() {
        assertEquals(
            """@<a href="@abc-123">abc-123</a>""",
            "@abc-123".linkifyTagsAndMentions(),
        )
        assertEquals(
            """to @<a href="@Ja-nieja-niktja">Ja-nieja-niktja</a> ,""",
            "to @Ja-nieja-niktja ,".linkifyTagsAndMentions(),
        )
    }

    @Test
    fun `trailing hyphen is not part of mention`() {
        assertEquals(
            """@<a href="@abc">abc</a>- reszta""",
            "@abc- reszta".linkifyTagsAndMentions(),
        )
    }

    @Test
    fun `hyphen ends a tag`() {
        assertEquals(
            """#<a href="#tag">tag</a>-cos""",
            "#tag-cos".linkifyTagsAndMentions(),
        )
    }

    @Test
    fun `bare urls are linkified without trailing punctuation`() {
        assertEquals(
            """zobacz <a href="https://wykop.pl/wpis/123">https://wykop.pl/wpis/123</a>.""",
            "zobacz https://wykop.pl/wpis/123.".linkifyTagsAndMentions(),
        )
    }

    @Test
    fun `html entities are not treated as tags`() {
        assertEquals("&#39;", "&#39;".linkifyTagsAndMentions())
    }

    @Test
    fun `existing anchors are not double-linkified`() {
        val html = """<a href="https://wykop.pl">@abc w linku</a> @def"""
        assertEquals(
            """<a href="https://wykop.pl">@abc w linku</a> @<a href="@def">def</a>""",
            html.linkifyTagsAndMentions(),
        )
    }

    @Test
    fun `markdown link and bare url in converted content`() {
        assertEquals(
            """<a href="https://x.pl">opis</a>""",
            "[opis](https://x.pl)".convertWykopContentToHtml(),
        )
    }
}

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
    fun `mention with leading and trailing hyphen is whole login`() {
        // Realny login z Wykopu: myslniki na obu brzegach nazwy.
        assertEquals(
            """@<a href="@-SCHlZOFRENlCYZM-">-SCHlZOFRENlCYZM-</a> dobrze""",
            "@-SCHlZOFRENlCYZM- dobrze".linkifyTagsAndMentions(),
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
    fun `tag and mention need whitespace or text start before them`() {
        // W srodku slowa nie linkujemy - najczestsza ofiara byly adresy e-mail.
        assertEquals("napisz na jan@example.com", "napisz na jan@example.com".linkifyTagsAndMentions())
        assertEquals("piszemy w C#", "piszemy w C#".linkifyTagsAndMentions())
        assertEquals("foo#bar", "foo#bar".linkifyTagsAndMentions())
        assertEquals("foo@bar", "foo@bar".linkifyTagsAndMentions())
    }

    @Test
    fun `tag at line start is linkified after html break`() {
        // Tresc jest juz HTML-em - tagi na koncu wpisu stoja zaraz po "<br>".
        assertEquals(
            """tresc<br>#<a href="#heheszki">heheszki</a>""",
            "tresc<br>#heheszki".linkifyTagsAndMentions(),
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

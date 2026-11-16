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

    @Test
    fun `italic next to special characters`() {
        assertEquals("(<i>kursywa</i>)", "(_kursywa_)".convertWykopContentToHtml())
        assertEquals("\"<i>cytat</i>\"", "\"_cytat_\"".convertWykopContentToHtml())
        assertEquals("a <i>b</i> c <i>d</i>.", "a _b_ c _d_.".convertWykopContentToHtml())
    }

    @Test
    fun `underscores inside words are not italic`() {
        assertEquals("text_text_text", "text_text_text".convertWykopContentToHtml())
        assertEquals("__init__", "__init__".convertWykopContentToHtml())
        assertEquals("snake_case i <i>kursywa</i>", "snake_case i _kursywa_".convertWykopContentToHtml())
    }

    @Test
    fun `italic combined with bold`() {
        assertEquals("<i><b>tekst</b></i>", "_**tekst**_".convertWykopContentToHtml())
        assertEquals("<b><i>tekst</i></b>", "**_tekst_**".convertWykopContentToHtml())
    }

    @Test
    fun `angle brackets in content are escaped, not parsed as tags`() {
        // Html.fromHtml bral "<cos>" i samotne "<" za znacznik - tekst znikal.
        assertEquals("a &lt;b&gt; c", "a <b> c".convertWykopContentToHtml())
        assertEquals("2 &lt; 3 i dalej", "2 < 3 i dalej".convertWykopContentToHtml())
        assertEquals("&lt;3 &gt;_&lt;", "<3 >_<".convertWykopContentToHtml())
    }

    @Test
    fun `quote marker still detected while its content is escaped`() {
        assertEquals("<blockquote>a &lt;b&gt;</blockquote>", "> a <b>".convertWykopContentToHtml())
        assertEquals("<blockquote>&gt; zagniezdzony</blockquote>", ">> zagniezdzony".convertWykopContentToHtml())
    }

    @Test
    fun `angle brackets escaped in spoilers and code`() {
        assertEquals("<spoiler>x &lt;y&gt;</spoiler>", "! x <y>".convertWykopContentToHtml())
        assertEquals("<code>List&lt;String&gt;</code>", "`List<String>`".convertWykopContentToHtml())
    }

    @Test
    fun `entities from api are kept for the html parser`() {
        assertEquals("to &#39;nic&#39; &amp; tyle", "to &#39;nic&#39; &amp; tyle".convertWykopContentToHtml())
    }

    @Test
    fun `url wrapped in angle brackets does not swallow the bracket`() {
        assertEquals(
            """&lt;<a href="https://x.pl/a">https://x.pl/a</a>&gt;""",
            "<https://x.pl/a>".convertWykopContentToHtml().linkifyTagsAndMentions(),
        )
    }

    @Test
    fun `plain api text decodes entities and keeps angle brackets`() {
        assertEquals("Ceny <spadaja> & 'rosna'", "Ceny <spadaja> &amp; &#39;rosna&#39;".decodeHtmlEntities())
        assertEquals("2 < 3 \"x\" é", "2 &lt; 3 &quot;x&quot; &#xE9;".decodeHtmlEntities())
        // Nieznane/niepelne encje i samotny "&" zostaja nietkniete.
        assertEquals("A & B &foo; &#;", "A & B &foo; &#;".decodeHtmlEntities())
    }

    @Test
    fun `underscores in urls and mentions are not italic`() {
        assertEquals("https://x.pl/_a_/b", "https://x.pl/_a_/b".convertWykopContentToHtml())
        assertEquals("@_nick_ hej", "@_nick_ hej".convertWykopContentToHtml())
    }
}

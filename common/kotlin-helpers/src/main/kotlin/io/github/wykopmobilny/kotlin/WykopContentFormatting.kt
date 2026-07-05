package io.github.wykopmobilny.kotlin

/**
 * Converts Wykop v3 markdown-like content to HTML for display.
 * Handles: line breaks, **bold**, _italic_, `code`, [text](url),
 * spoilers (lines starting with !), and quotes (lines starting with >).
 */
fun String.convertWykopContentToHtml(): String {
    if (isBlank()) return this

    // Extract code blocks first to protect their content from formatting
    val codeBlocks = mutableListOf<String>()
    val withCodePlaceholders =
        Regex("`([^`]+)`").replace(this) { match ->
            codeBlocks.add(match.groupValues[1])
            "\u0000CODE${codeBlocks.size - 1}\u0000"
        }

    // Process line by line for block-level elements
    val lines = withCodePlaceholders.split("\n")
    val htmlLines = mutableListOf<String>()
    val quoteBuffer = mutableListOf<String>()

    fun flushQuote() {
        if (quoteBuffer.isNotEmpty()) {
            // Kolejne linie cytatu scalone w JEDEN blok - osobne <blockquote>
            // renderowaly sie jako niezalezne kreski z odstepami.
            htmlLines += "<blockquote>${quoteBuffer.joinToString("<br>")}</blockquote>"
            quoteBuffer.clear()
        }
    }

    for ((index, line) in lines.withIndex()) {
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith(">") -> {
                quoteBuffer += trimmed.removePrefix(">").trim()
            }

            // Pusta linia miedzy liniami cytatu nalezy do cytatu - nie rozbija bloku.
            trimmed.isBlank() &&
                quoteBuffer.isNotEmpty() &&
                lines.getOrNull(index + 1)?.trimStart()?.startsWith(">") == true -> {
                quoteBuffer += ""
            }

            trimmed.startsWith("!") && trimmed.length > 1 -> {
                flushQuote()
                val spoilerContent = trimmed.removePrefix("!").trim()
                htmlLines += "<spoiler>$spoilerContent</spoiler>"
            }

            else -> {
                flushQuote()
                htmlLines += line
            }
        }
    }
    flushQuote()

    var html =
        htmlLines
            .joinToString("<br>")
            // Blockquote sam tworzy odstep akapitowy - <br> obok niego dublowalby przerwe.
            .replace("<br><blockquote>", "<blockquote>")
            .replace("</blockquote><br>", "</blockquote>")

    // Bold: **text**
    html = Regex("\\*\\*(.+?)\\*\\*").replace(html) { "<b>${it.groupValues[1]}</b>" }

    // Italic: _text_ (only at word boundaries to avoid matching inside URLs/identifiers)
    html =
        Regex("(^|[\\s>])_(.+?)_([\\s<.,;:!?]|$)", RegexOption.MULTILINE).replace(html) {
            "${it.groupValues[1]}<i>${it.groupValues[2]}</i>${it.groupValues[3]}"
        }

    // Link: [text](url)
    html =
        Regex("\\[([^]]+)]\\(([^)]+)\\)").replace(html) {
            "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>"
        }

    // Restore code blocks
    for (i in codeBlocks.indices) {
        html = html.replace("\u0000CODE$i\u0000", "<code>${codeBlocks[i]}</code>")
    }

    return html
}

/**
 * Wraps #tags, @mentions and bare http(s) URLs in <a> tags so they become clickable links.
 * Skips text that is already inside existing <a>...</a> tags to avoid double-linkification.
 */
fun String.linkifyTagsAndMentions(): String {
    val linkPattern = Regex("<a\\s[^>]*>.*?</a>", RegexOption.DOT_MATCHES_ALL)
    val result = StringBuilder()
    var lastEnd = 0

    for (match in linkPattern.findAll(this)) {
        result.append(linkifyPlainText(substring(lastEnd, match.range.first)))
        result.append(match.value)
        lastEnd = match.range.last + 1
    }
    result.append(linkifyPlainText(substring(lastEnd)))

    return result.toString()
}

// Loginy moga zawierac myslniki w srodku (np. @Ja-nieja-niktja), tagi juz nie -
// myslnik konczy tag. Myslnik na koncu (np. "@abc-") nie wchodzi do wzmianki.
private val linkableRegex =
    Regex(
        "(https?://[^\\s<>\"]+)" +
            "|(?<!&)(#)(\\w+)" +
            "|(@)(\\w+(?:-\\w+)*)",
    )

// Znaki interpunkcyjne po URL-u ("zobacz https://x.pl/a.") nie sa jego czescia.
private val urlTrailingPunctuation = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '"', '\'', '…')

private fun linkifyPlainText(text: String): String =
    linkableRegex.replace(text) { match ->
        val url = match.groupValues[1]
        if (url.isNotEmpty()) {
            val trimmed = url.trimEnd(*urlTrailingPunctuation)
            val rest = url.substring(trimmed.length)
            "<a href=\"$trimmed\">$trimmed</a>$rest"
        } else {
            val prefix = match.groupValues[2].ifEmpty { match.groupValues[4] }
            val name = match.groupValues[3].ifEmpty { match.groupValues[5] }
            "$prefix<a href=\"$prefix$name\">$name</a>"
        }
    }

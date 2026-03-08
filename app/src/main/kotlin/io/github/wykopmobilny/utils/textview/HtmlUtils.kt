package io.github.wykopmobilny.utils.textview

import android.text.Spannable
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Pattern

fun String.toSpannable(): Spannable = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT, null, CodeTagHandler()).toSpannable()

fun String.removeHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

/**
 * Converts Wykop v3 markdown-like content to HTML for display.
 * Handles: line breaks, **bold**, _italic_, `code`, [text](url),
 * spoilers (lines starting with !), and quotes (lines starting with >).
 */
fun String.convertWykopContentToHtml(): String {
    if (isBlank()) return this

    // Extract code blocks first to protect their content from formatting
    val codeBlocks = mutableListOf<String>()
    var withCodePlaceholders =
        Regex("`([^`]+)`").replace(this) { match ->
            codeBlocks.add(match.groupValues[1])
            "\u0000CODE${codeBlocks.size - 1}\u0000"
        }

    // Process line by line for block-level elements
    val lines = withCodePlaceholders.split("\n")
    val htmlLines =
        lines.map { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("!") && trimmed.length > 1 -> {
                    val spoilerContent = trimmed.removePrefix("!").trim()
                    val encoded = URLEncoder.encode(spoilerContent, "UTF-8")
                    "<a href=\"spoiler:$encoded\">[pokaż spoiler]</a>"
                }

                trimmed.startsWith(">") -> {
                    val quoteContent = trimmed.removePrefix(">").trim()
                    "<blockquote>$quoteContent</blockquote>"
                }

                else -> {
                    line
                }
            }
        }

    var html = htmlLines.joinToString("<br>")

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

fun String.removeSpoilerHtml(): String {
    val regexBegin = "<a href=\"spoiler:"
    val matcher =
        Pattern
            .compile("($regexBegin).*?\">\\[pokaż spoiler]</a>")
            .matcher(this)
    val matches = ArrayList<String>()
    var fullstring = this
    while (matcher.find()) {
        matches.add(matcher.group())
    }

    matches.forEach {
        val text =
            "! " +
                URLDecoder.decode(
                    it.replace(regexBegin, "").replace("\">[pokaż spoiler]</a>", ""),
                    "UTF-8",
                )
        fullstring = fullstring.replaceFirst(it, text)
    }

    return fullstring
}

fun String.stripWykopFormatting(): String =
    if (contains("<a href=\"spoiler:")) {
        removeSpoilerHtml().removeHtml()
    } else {
        removeHtml()
    }

/**
 * Wraps #tags and @mentions in <a> tags so they become clickable links.
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

private fun linkifyPlainText(text: String): String {
    val tagMentionRegex = Regex("(?<!&)([#@])(\\w+)")
    return tagMentionRegex.replace(text) { match ->
        val prefix = match.groupValues[1]
        val name = match.groupValues[2]
        "$prefix<a href=\"${match.value}\">$name</a>"
    }
}

package io.github.wykopmobilny.utils.textview

import android.text.Spannable
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import java.net.URLDecoder
import java.util.regex.Pattern

fun String.toSpannable(): Spannable = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT, null, CodeTagHandler()).toSpannable()

fun String.removeHtml() = HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

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

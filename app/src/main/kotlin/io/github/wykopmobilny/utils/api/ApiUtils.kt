package io.github.wykopmobilny.utils.api

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import io.github.wykopmobilny.R
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun parseDate(date: String): Date? {
    val format = SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.GERMANY)
    format.timeZone = TimeZone.getTimeZone("Europe/Warsaw")
    return format.parse(date)
}

/**
 * Maps v3 API color name to a stable group ID used by [getGroupColor].
 * Color definitions from /v3/config endpoint.
 */
fun colorNameToGroupId(name: String?): Int =
    when (name) {
        "green" -> 0
        "orange" -> 1
        "burgundy" -> 2
        "purple" -> 3
        "red" -> 4
        "black" -> 5
        else -> 0
    }

fun getGroupColor(
    role: Int,
    isUsingDarkTheme: Boolean = true,
): Int =
    when (role) {
        0 -> Color.parseColor("#339933")
        1 -> Color.parseColor("#ff5917")
        2 -> if (isUsingDarkTheme) Color.parseColor("#bb1111") else Color.parseColor("#990000")
        3 -> if (isUsingDarkTheme) Color.parseColor("#694797") else Color.parseColor("#593787")
        4 -> Color.parseColor("#d81e04")
        5 -> if (isUsingDarkTheme) Color.parseColor("#ffffff") else Color.parseColor("#000000")
        999 -> Color.parseColor("#BF9B30")
        1001 -> Color.parseColor("#999999")
        1002 -> Color.parseColor("#999999")
        2001 -> Color.parseColor("#3F6FA0")
        else -> Color.BLUE
    }

fun Context.getGroupColor(role: Int): Int {
    val isDark =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    return when (role) {
        0 -> {
            Color.parseColor("#339933")
        }

        1 -> {
            Color.parseColor("#ff5917")
        }

        2 -> {
            if (isDark) Color.parseColor("#bb1111") else Color.parseColor("#990000")
        }

        3 -> {
            if (isDark) Color.parseColor("#694797") else Color.parseColor("#593787")
        }

        4 -> {
            Color.parseColor("#d81e04")
        }

        5 -> {
            val tv = TypedValue()
            theme.resolveAttribute(R.attr.adminNickColor, tv, true)
            tv.data
        }

        999 -> {
            Color.parseColor("#BF9B30")
        }

        1001 -> {
            Color.parseColor("#999999")
        }

        1002 -> {
            Color.parseColor("#999999")
        }

        2001 -> {
            Color.parseColor("#3F6FA0")
        }

        else -> {
            Color.BLUE
        }
    }
}

fun getGenderStripResource(authorSex: String): Int =
    when (authorSex) {
        "male" -> R.drawable.strip_male
        "female" -> R.drawable.strip_female
        else -> 0
    }

fun String.stripImageCompression(): String {
    val extension = substringAfterLast(".")
    val baseUrl = substringBeforeLast(",")
    return baseUrl + if (!baseUrl.endsWith(extension)) ".$extension" else ""
}

fun String.convertMarkdownToHtml(): String {
    val flavour = WykopFlavorDescriptor()
    val parsedTree =
        MarkdownParser(flavour)
            .buildMarkdownTreeFromString(this)
    var html = HtmlGenerator(this, parsedTree, flavour).generateHtml()
    val regex = Regex("[#@]\\w+")
    regex.findAll(html).forEach {
        html = html.replace(it.value, "${it.value[0]}<a href=\"${it.value}\">${it.value.removePrefix("${it.value[0]}")}</a>")
    }
    return html.removePrefix("<body><p>").removeSuffix("</p></body>")
}

fun String.encryptMD5(): String {
    val bytes = toByteArray()
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(bytes)
    var result = ""
    for (byte in digest) result += "%02x".format(byte)
    return result
}

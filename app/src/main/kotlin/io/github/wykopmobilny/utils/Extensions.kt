package io.github.wykopmobilny.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import io.github.wykopmobilny.utils.api.parseDate
import io.github.wykopmobilny.utils.recyclerview.ViewHolderDependentItemDecorator
import kotlinx.datetime.Instant
import org.ocpsoft.prettytime.PrettyTime
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

fun SpannableStringBuilder.appendNewSpan(
    text: CharSequence,
    what: Any,
    flags: Int,
): SpannableStringBuilder {
    val start = length
    append(text)
    setSpan(what, start, length, flags)
    return this
}

fun View.getActivityContext(): Activity? {
    var context = context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun RecyclerView.prepare() {
    setItemViewCacheSize(20)
    layoutManager = LinearLayoutManager(context)
    (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
    addItemDecoration(ViewHolderDependentItemDecorator(context))
}

fun RecyclerView.prepareNoDivider() {
    setItemViewCacheSize(20)
    layoutManager = LinearLayoutManager(context)
}

/**
 * Transformuje URL obrazka na format z miniaturą dla widoków głównych.
 * Format: [adres bez rozszerzenia],w220h142[rozszerzenie]
 *
 * Przykład:
 * "https://example.com/image.jpg" -> "https://example.com/image,w220h142.jpg"
 */
fun String.toThumbnailUrl(): String {
    val lastDotIndex = lastIndexOf('.')
    return if (lastDotIndex > 0) {
        "${substring(0, lastDotIndex)},w220h142${substring(lastDotIndex)}"
    } else {
        this // Jeśli nie ma rozszerzenia, zwróć oryginalny URL
    }
}

fun ImageView.loadImage(
    url: String,
    signature: Int? = null,
) {
    if (signature == null) {
        Glide
            .with(context)
            .load(url)
            .into(this)
    } else {
        Glide
            .with(context)
            .load(url)
            .apply(
                RequestOptions()
                    .signature(ObjectKey(signature)),
            ).into(this)
    }
}

/**
 * Ładuje obrazek w formie miniatury (w220h142) dla widoków głównych
 * (Strona Główna, Wykopalisko, Hity).
 */
fun ImageView.loadImageThumbnail(
    url: String,
    signature: Int? = null,
) {
    loadImage(url.toThumbnailUrl(), signature)
}

fun String.toPrettyDate(): String = PrettyTime(Locale("pl")).format(parseDate(this))

fun Instant.toPrettyDate(): String = PrettyTime(Locale("pl")).format(Date(toEpochMilliseconds()))

fun Uri.queryFileName(contentResolver: ContentResolver): String {
    var result: String? = null
    if (scheme == "content") {
        result =
            contentResolver.query(this, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
    }
    if (result == null) {
        result = path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

/**
 * Parse YouTube timestamp from string to milliseconds value.
 *
 * Original value comes from 't' parameter from youtube url and is given in seconds. YouTube player needs this
 * value in milliseconds.
 *
 * Parameter 't' can be in following formats:
 * '3' we can assume that this values is in seconds
 * '3m', '5m30s', '1h30m30s' needs conversion using ISO 8601.
 *
 * @return time in milliseconds, null if value cannot be converted.
 *
 */
internal fun String.youtubeTimestampToMsOrNull(): Long? {
    val timestamp = this.toLongOrNull()
    if (timestamp != null) {
        return timestamp.seconds.inWholeMilliseconds
    }

    return try {
        Duration.parse("PT$this").toMillis()
    } catch (e: DateTimeParseException) {
        null
    }
}

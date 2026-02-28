package io.github.wykopmobilny.wykop.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val apiTimeZone = TimeZone.of("Europe/Warsaw")

internal class InstantAdapter {
    @ToJson
    fun toJson(date: Instant?) = date?.toString()

    @FromJson
    fun fromJson(date: String) =
        runCatching {
            // Try ISO 8601 format with timezone (e.g., "2026-02-28T07:26:37Z")
            Instant.parse(date)
        }.recoverCatching {
            // Fallback: try parsing as LocalDateTime and convert to Instant with API timezone
            kotlinx.datetime.LocalDateTime
                .parse(date.replace(' ', 'T'))
                .toInstant(apiTimeZone)
        }.recoverCatching {
            // Second fallback: try without space replacement
            kotlinx.datetime.LocalDateTime
                .parse(date)
                .toInstant(apiTimeZone)
        }.onFailure {
            Napier.e("Could not parse date: $date", it)
        }.getOrNull()
}

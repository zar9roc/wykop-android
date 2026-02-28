package io.github.wykopmobilny.api.responses.v3.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class DateTimeAdapter {
    @FromJson
    fun fromJson(value: String): Instant =
        runCatching {
            // Try ISO 8601 format with timezone (e.g., "2026-02-28T07:26:37Z")
            Instant.parse(value)
        }.recoverCatching {
            // Fallback: try parsing as LocalDateTime with UTC timezone
            kotlinx.datetime.LocalDateTime
                .parse(value.replace(" ", "T"))
                .toInstant(TimeZone.UTC)
        }.getOrThrow()

    @ToJson
    fun toJson(value: Instant): String =
        value
            .toLocalDateTime(TimeZone.UTC)
            .toString()
            .replace("T", " ")
            .substringBefore('.')
}

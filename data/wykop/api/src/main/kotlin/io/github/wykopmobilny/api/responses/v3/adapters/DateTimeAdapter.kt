package io.github.wykopmobilny.api.responses.v3.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class DateTimeAdapter {
    @FromJson
    fun fromJson(value: String): Instant {
        val localDateTime =
            kotlinx.datetime.LocalDateTime.parse(
                value.replace(" ", "T"),
            )
        return localDateTime.toInstant(TimeZone.UTC)
    }

    @ToJson
    fun toJson(value: Instant): String =
        value
            .toLocalDateTime(TimeZone.UTC)
            .toString()
            .replace("T", " ")
            .substringBefore('.')
}

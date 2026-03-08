package io.github.wykopmobilny.api.responses.v3.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.lang.reflect.Type

/**
 * Moshi adapter for kotlin.Unit.
 * Required for Retrofit endpoints that return WykopApiResponseV3<Unit>
 * (vote, delete, edit operations where API returns no meaningful data).
 */
class UnitJsonAdapter : JsonAdapter<Unit>() {
    override fun fromJson(reader: JsonReader) {
        reader.skipValue()
    }

    override fun toJson(
        writer: JsonWriter,
        value: Unit?,
    ) {
        writer.nullValue()
    }

    companion object {
        val FACTORY =
            object : Factory {
                override fun create(
                    type: Type,
                    annotations: Set<Annotation>,
                    moshi: Moshi,
                ): JsonAdapter<*>? {
                    if (type == Unit::class.java) {
                        return UnitJsonAdapter()
                    }
                    return null
                }
            }
    }
}

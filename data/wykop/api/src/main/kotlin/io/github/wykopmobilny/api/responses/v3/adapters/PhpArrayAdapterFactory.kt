package io.github.wykopmobilny.api.responses.v3.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * Handles PHP's json_encode quirk where arrays with non-sequential integer keys
 * are serialized as JSON objects instead of arrays.
 * E.g. PHP: [5 => "a", 10 => "b"] → {"5":"a","10":"b"} instead of ["a","b"]
 */
class PhpArrayAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != List::class.java) return null
        if (annotations.isNotEmpty()) return null

        val delegate = moshi.nextAdapter<List<*>>(this, type, annotations)
        val elementType = Types.collectionElementType(type, List::class.java)
        val elementAdapter = moshi.adapter<Any>(elementType)

        return PhpArrayAdapter(delegate, elementAdapter)
    }
}

private class PhpArrayAdapter<T>(
    private val delegate: JsonAdapter<List<T>>,
    private val elementAdapter: JsonAdapter<T>,
) : JsonAdapter<List<T>>() {

    override fun fromJson(reader: JsonReader): List<T>? {
        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            val result = mutableListOf<T>()
            reader.beginObject()
            while (reader.hasNext()) {
                reader.skipName()
                @Suppress("UNCHECKED_CAST")
                (elementAdapter.fromJson(reader) as? T)?.let { result.add(it) }
            }
            reader.endObject()
            return result
        }
        return delegate.fromJson(reader)
    }

    override fun toJson(writer: JsonWriter, value: List<T>?) {
        delegate.toJson(writer, value)
    }
}

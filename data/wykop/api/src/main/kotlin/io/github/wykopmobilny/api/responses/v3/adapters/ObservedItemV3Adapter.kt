package io.github.wykopmobilny.api.responses.v3.adapters

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.wykopmobilny.api.responses.v3.entries.EntryResponseV3
import io.github.wykopmobilny.api.responses.v3.links.LinkResponseV3
import io.github.wykopmobilny.api.responses.v3.observed.ObservedItemV3
import okio.Buffer

class ObservedItemV3Adapter(
    moshi: Moshi,
) : JsonAdapter<ObservedItemV3>() {
    private val entryAdapter: JsonAdapter<EntryResponseV3> = moshi.adapter(EntryResponseV3::class.java)
    private val linkAdapter: JsonAdapter<LinkResponseV3> = moshi.adapter(LinkResponseV3::class.java)
    private val mapAdapter: JsonAdapter<Map<String, Any?>> =
        moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

    override fun fromJson(reader: JsonReader): ObservedItemV3? {
        @Suppress("UNCHECKED_CAST")
        val jsonValue = reader.readJsonValue() as? Map<String, Any?> ?: return null
        val resource = jsonValue["resource"] as? String ?: return null
        val json = mapAdapter.toJson(jsonValue)
        val source = Buffer().writeUtf8(json)
        return when (resource) {
            "entry" -> entryAdapter.fromJson(source)?.let { ObservedItemV3.EntryItem(it) }
            "link" -> linkAdapter.fromJson(source)?.let { ObservedItemV3.LinkItem(it) }
            else -> null
        }
    }

    override fun toJson(
        writer: JsonWriter,
        value: ObservedItemV3?,
    ) {
        when (value) {
            is ObservedItemV3.EntryItem -> entryAdapter.toJson(writer, value.entry)
            is ObservedItemV3.LinkItem -> linkAdapter.toJson(writer, value.link)
            null -> writer.nullValue()
        }
    }

    companion object {
        val FACTORY =
            Factory { type, _, moshi ->
                if (Types.getRawType(type) == ObservedItemV3::class.java) {
                    ObservedItemV3Adapter(moshi)
                } else {
                    null
                }
            }
    }
}

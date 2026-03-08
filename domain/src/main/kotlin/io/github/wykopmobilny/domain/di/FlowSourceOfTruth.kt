package io.github.wykopmobilny.domain.di

import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.SourceOfTruth

/**
 * Creates a [SourceOfTruth] with a Flow-based reader.
 * Workaround for Kotlin 2.2 compatibility with Store5's SourceOfTruth.ofFlow companion method.
 */
internal fun <Key : Any, Local : Any, Output : Any> flowSourceOfTruth(
    reader: (Key) -> Flow<Output>,
    writer: suspend (Key, Local) -> Unit,
    delete: suspend (Key) -> Unit = {},
    deleteAll: suspend () -> Unit = {},
): SourceOfTruth<Key, Local, Output> = object : SourceOfTruth<Key, Local, Output> {
    override fun reader(key: Key): Flow<Output> = reader(key)
    override suspend fun write(key: Key, value: Local) = writer(key, value)
    override suspend fun delete(key: Key) = delete(key)
    override suspend fun deleteAll() = deleteAll()
}

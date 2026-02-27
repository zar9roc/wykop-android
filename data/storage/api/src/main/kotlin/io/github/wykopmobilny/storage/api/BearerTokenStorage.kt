package io.github.wykopmobilny.storage.api

import kotlinx.coroutines.flow.Flow

interface BearerTokenStorage {
    val bearerToken: Flow<String?>

    suspend fun updateBearerToken(value: String?)
}

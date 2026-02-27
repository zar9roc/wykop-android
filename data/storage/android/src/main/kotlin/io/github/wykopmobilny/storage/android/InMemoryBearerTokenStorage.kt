package io.github.wykopmobilny.storage.android

import io.github.wykopmobilny.storage.api.BearerTokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal class InMemoryBearerTokenStorage
    @Inject
    constructor() : BearerTokenStorage {
        private val _bearerToken = MutableStateFlow<String?>(null)

        override val bearerToken: Flow<String?> = _bearerToken.asStateFlow()

        override suspend fun updateBearerToken(value: String?) {
            _bearerToken.value = value
        }
    }

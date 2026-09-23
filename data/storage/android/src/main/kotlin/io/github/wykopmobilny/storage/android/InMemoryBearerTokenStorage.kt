package io.github.wykopmobilny.storage.android

import io.github.wykopmobilny.storage.api.BearerTokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal class InMemoryBearerTokenStorage
    @Inject
    constructor() : BearerTokenStorage {
        private val _bearerToken = MutableStateFlow<String?>(null)
        private val _authAttempts = MutableStateFlow(0)

        override val bearerToken: Flow<String?> = _bearerToken.asStateFlow()

        override val authAttempts: Flow<Int> = _authAttempts.asStateFlow()

        override suspend fun updateBearerToken(value: String?) {
            _bearerToken.value = value
        }

        override suspend fun onAuthAttemptFinished() {
            _authAttempts.update { it + 1 }
        }
    }

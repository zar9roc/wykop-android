package io.github.wykopmobilny.storage.api

import kotlinx.coroutines.flow.Flow

interface BearerTokenStorage {
    val bearerToken: Flow<String?>

    /**
     * Licznik zakonczonych prob POST /v3/auth - rosnie takze po probie nieudanej.
     * Dzieki temu interceptor czekajacy na token wie, kiedy przestac czekac:
     * gdy proba padla (np. brak sieci), token juz nie przyjdzie.
     */
    val authAttempts: Flow<Int>

    suspend fun updateBearerToken(value: String?)

    /** Do wywolania po kazdej zakonczonej probie auth - udanej i nieudanej. */
    suspend fun onAuthAttemptFinished()
}

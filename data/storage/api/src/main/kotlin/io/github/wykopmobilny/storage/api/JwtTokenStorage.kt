package io.github.wykopmobilny.storage.api

import kotlinx.coroutines.flow.Flow

interface JwtTokenStorage {
    val jwtToken: Flow<JwtToken?>

    suspend fun updateJwtToken(value: JwtToken?)
}

data class JwtToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
)

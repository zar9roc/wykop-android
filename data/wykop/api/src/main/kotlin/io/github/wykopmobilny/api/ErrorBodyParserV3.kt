package io.github.wykopmobilny.api

import io.github.wykopmobilny.api.responses.v3.common.WykopErrorResponseV3
import okhttp3.ResponseBody

/**
 * Parser for API v3 error response bodies.
 *
 * API v3 returns errors with HTTP 4xx/5xx status codes and a different structure than v2:
 * ```json
 * {
 *   "code": 401,
 *   "hash": "abc123",
 *   "error": {
 *     "message": "Error message",
 *     "key": 1001
 *   }
 * }
 * ```
 */
interface ErrorBodyParserV3 {
    suspend fun parse(body: ResponseBody): WykopErrorResponseV3?
}

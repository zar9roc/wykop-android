package io.github.wykopmobilny.debug

import io.github.aakira.napier.Napier
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Diagnostic checkpoint logger for verifying UI/layout changes at runtime.
 *
 * Usage in code:
 * ```
 * DiagnosticCheckpoint.log("LinkDetails", "Header loaded with title: ${link.title}")
 * DiagnosticCheckpoint.log("EntryVote", "Vote toggled: voted=$isVoted, count=$voteCount")
 * ```
 *
 * Reading checkpoints via debug HTTP server:
 * ```
 * adb forward tcp:8899 tcp:8899
 * curl http://localhost:8899/checkpoints
 * curl -X DELETE http://localhost:8899/checkpoints   # clear all
 * curl "http://localhost:8899/checkpoints?tag=LinkDetails"  # filter by tag
 * ```
 *
 * Reading via logcat:
 * ```
 * adb logcat -s DiagnosticCheckpoint
 * ```
 */
object DiagnosticCheckpoint {

    private const val TAG = "DiagnosticCheckpoint"
    private const val MAX_CHECKPOINTS = 200

    private val checkpoints = CopyOnWriteArrayList<CheckpointEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    data class CheckpointEntry(
        val timestamp: Long,
        val tag: String,
        val message: String,
        val threadName: String,
    )

    /**
     * Log a diagnostic checkpoint.
     *
     * @param tag Short identifier for the feature/screen (e.g. "LinkDetails", "EntryVote")
     * @param message Description of what happened (e.g. "Header loaded with 5 comments")
     */
    fun log(tag: String, message: String) {
        val entry = CheckpointEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message,
            threadName = Thread.currentThread().name,
        )
        checkpoints.add(entry)
        Napier.d("[$tag] $message", tag = TAG)

        // Trim oldest entries if over limit
        while (checkpoints.size > MAX_CHECKPOINTS) {
            checkpoints.removeAt(0)
        }
    }

    /**
     * Get all checkpoints as JSON, optionally filtered by tag.
     */
    fun toJson(filterTag: String? = null): JSONObject {
        val filtered = if (filterTag.isNullOrBlank()) {
            checkpoints.toList()
        } else {
            checkpoints.filter { it.tag.equals(filterTag, ignoreCase = true) }
        }

        return JSONObject().apply {
            put("count", filtered.size)
            put("filter_tag", filterTag ?: JSONObject.NULL)
            put("checkpoints", JSONArray().apply {
                for (entry in filtered) {
                    put(JSONObject().apply {
                        put("timestamp", dateFormat.format(Date(entry.timestamp)))
                        put("timestamp_ms", entry.timestamp)
                        put("tag", entry.tag)
                        put("message", entry.message)
                        put("thread", entry.threadName)
                    })
                }
            })
        }
    }

    /**
     * Clear all stored checkpoints.
     */
    fun clear(): JSONObject {
        val count = checkpoints.size
        checkpoints.clear()
        Napier.d("Cleared $count checkpoints", tag = TAG)
        return JSONObject().apply {
            put("action", "clear_checkpoints")
            put("cleared_count", count)
        }
    }
}

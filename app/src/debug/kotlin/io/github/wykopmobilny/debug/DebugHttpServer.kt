package io.github.wykopmobilny.debug

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import fi.iki.elonen.NanoHTTPD
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.adapter.EndlessProgressAdapter
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.models.dataclass.EntryLink
import io.github.wykopmobilny.models.dataclass.Link
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.ui.modules.links.relatedlinks.RelatedLinksActivity

/**
 * Debug HTTP server exposing app state and actions via REST API.
 * Uses NanoHTTPD on port [PORT]. Access via:
 *   adb forward tcp:8899 tcp:8899 && curl http://localhost:8899/state
 *
 * Endpoints documented at GET /
 */
class DebugHttpServer(
    private val appContext: Context,
    private val entriesApi: EntriesV3RetrofitApi,
    private val linksApi: LinksV3RetrofitApi,
) : NanoHTTPD(PORT) {

    companion object {
        private const val TAG = "DebugHttpServer"
        const val PORT = 8899
        private const val MAIN_THREAD_TIMEOUT_SECONDS = 5L
        private val VOTE_ENTRY_REGEX = Regex("/action/vote/entry/\\d+")
        private val VOTE_RELATED_REGEX = Regex("/action/vote/related/\\d+/\\d+/(up|down)")
        private val LIST_RELATED_REGEX = Regex("/action/list/related/\\d+")
        private val OPEN_RELATED_REGEX = Regex("/action/open/related/\\d+")
        private val OPEN_LINK_REGEX = Regex("/action/open/link/\\d+")
        private val OPEN_ENTRY_REGEX = Regex("/action/open/entry/\\d+")
        private val AVAILABLE_TABS = listOf(
            "promoted", "upcoming", "hits", "hot",
            "mywykop", "favorite", "search", "messages", "notifications",
        )
    }

    override fun start() {
        try {
            super.start()
            Napier.i("Debug HTTP server started on port $PORT (thread=${Thread.currentThread().name})", tag = TAG)
        } catch (e: java.io.IOException) {
            Napier.e("CRITICAL: Failed to start debug HTTP server on port $PORT", e, tag = TAG)
            throw e
        }
    }

    override fun stop() {
        super.stop()
        Napier.w("Debug HTTP server stopped on port $PORT", tag = TAG)
    }

    /**
     * Check if the server is alive and running.
     * Exposes NanoHTTPD's protected `isAlive()` method.
     */
    fun isRunning(): Boolean = isAlive

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                method == Method.GET && uri == "/" -> handleIndex()
                method == Method.GET && uri == "/state" -> handleState(session)
                method == Method.GET && uri == "/screen" -> handleScreen()
                method == Method.GET && uri == "/screen/entries" -> handleScreenEntries()
                method == Method.GET && uri == "/screen/links" -> handleScreenLinks()
                method == Method.POST && uri.startsWith("/navigate/") -> handleNavigate(uri)
                method == Method.POST && uri.matches(VOTE_ENTRY_REGEX) ->
                    handleVoteEntry(uri, vote = true)
                method == Method.DELETE && uri.matches(VOTE_ENTRY_REGEX) ->
                    handleVoteEntry(uri, vote = false)
                method == Method.POST && uri.matches(VOTE_RELATED_REGEX) ->
                    handleVoteRelated(uri)
                method == Method.GET && uri.matches(VOTE_RELATED_REGEX) ->
                    handleVoteRelated(uri)
                method == Method.GET && uri.matches(LIST_RELATED_REGEX) ->
                    handleListRelatedLinks(uri)
                method == Method.POST && uri.matches(OPEN_RELATED_REGEX) ->
                    handleOpenRelatedLinks(uri)
                method == Method.POST && uri.matches(OPEN_LINK_REGEX) ->
                    handleOpenLink(uri)
                method == Method.POST && uri.matches(OPEN_ENTRY_REGEX) ->
                    handleOpenEntry(uri)
                method == Method.POST && uri == "/action/clear-cache" -> handleClearCache()
                method == Method.POST && uri == "/action/logout" -> handleLogout()
                method == Method.GET && uri == "/checkpoints" -> handleCheckpoints(session)
                method == Method.DELETE && uri == "/checkpoints" -> handleClearCheckpoints()
                // GET convenience for browser
                method == Method.GET && uri.matches(OPEN_RELATED_REGEX) ->
                    handleOpenRelatedLinks(uri)
                method == Method.GET && uri.matches(OPEN_LINK_REGEX) ->
                    handleOpenLink(uri)
                method == Method.GET && uri.matches(OPEN_ENTRY_REGEX) ->
                    handleOpenEntry(uri)
                method == Method.GET && uri == "/action/clear-cache" -> handleClearCache()
                method == Method.GET && uri == "/action/clear-checkpoints" -> handleClearCheckpoints()
                method == Method.GET && uri.startsWith("/navigate/") -> handleNavigate(uri)
                else -> jsonResponse(
                    Response.Status.NOT_FOUND,
                    JSONObject().put("error", "Unknown endpoint: $method $uri"),
                )
            }
        } catch (e: Exception) {
            Napier.w("Error handling $method $uri", e, tag = TAG)
            jsonResponse(
                Response.Status.INTERNAL_ERROR,
                JSONObject().apply {
                    put("error", e.message ?: "Internal error")
                    put("path", "$method $uri")
                },
            )
        }
    }

    // --- Endpoints ---

    private fun handleIndex(): Response {
        val json = JSONObject().apply {
            put("server", "DebugHttpServer")
            put("port", PORT)
            put("endpoints", JSONArray().apply {
                put(endpoint("GET", "/", "This index"))
                put(endpoint("GET", "/state", "App state (activity, fragment, user)"))
                put(endpoint("GET", "/screen", "Current screen summary"))
                put(endpoint("GET", "/screen/entries", "Entry list from current adapter"))
                put(endpoint("GET", "/screen/links", "Link list from current adapter"))
                put(endpoint("POST", "/navigate/{tab}", "Switch to tab"))
                put(endpoint("POST", "/action/vote/entry/{id}", "Vote on entry"))
                put(endpoint("DELETE", "/action/vote/entry/{id}", "Unvote entry"))
                put(endpoint(
                    "POST",
                    "/action/vote/related/{linkId}/{relatedId}/{up|down}",
                    "Vote on related link",
                ))
                put(endpoint(
                    "GET",
                    "/action/list/related/{linkId}",
                    "List related links with IDs for a link",
                ))
                put(endpoint(
                    "POST",
                    "/action/open/related/{linkId}",
                    "Open related links screen for link",
                ))
                put(endpoint("POST", "/action/open/link/{id}", "Open link detail by ID"))
                put(endpoint("POST", "/action/open/entry/{id}", "Open entry detail by ID"))
                put(endpoint("POST", "/action/clear-cache", "Clear app cache"))
                put(endpoint("POST", "/action/logout", "Force logout"))
                put(endpoint("GET", "/checkpoints", "Diagnostic checkpoints (?tag=X to filter)"))
                put(endpoint("DELETE", "/checkpoints", "Clear all diagnostic checkpoints"))
            })
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleState(session: IHTTPSession): Response {
        val verbose = session.parms?.get("verbose")?.toBoolean() ?: false
        val json = runOnMainThread {
            DebugStateHelper.buildState(appContext, verbose).apply {
                put("server_port", PORT)
            }
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleScreen(): Response {
        val json = runOnMainThread {
            val activity = DebugActivityTracker.currentActivity
            val result = JSONObject()
            result.put("activity", activity?.javaClass?.simpleName ?: "none")

            if (activity is FragmentActivity) {
                val fragment = findVisibleLeafFragment(activity)
                result.put("fragment", fragment?.javaClass?.simpleName ?: "none")

                val recyclerView = fragment?.view?.findViewById<RecyclerView>(R.id.recyclerView)
                val adapter = recyclerView?.adapter
                if (adapter != null) {
                    result.put("adapter", adapter.javaClass.simpleName)
                    result.put("item_count", adapter.itemCount)
                    result.put("item_type", guessItemType(adapter))
                } else {
                    result.put("adapter", JSONObject.NULL)
                    result.put("item_count", 0)
                }
            }
            result
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleScreenEntries(): Response {
        val json = runOnMainThread {
            val activity = DebugActivityTracker.currentActivity
            val result = JSONObject()

            if (activity is FragmentActivity) {
                val fragment = findVisibleLeafFragment(activity)
                result.put("screen", fragment?.javaClass?.simpleName ?: "none")
                val entries = extractEntries(fragment)
                result.put("count", entries.length())
                result.put("entries", entries)
            } else {
                result.put("error", "No FragmentActivity in foreground")
            }
            result
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleScreenLinks(): Response {
        val json = runOnMainThread {
            val activity = DebugActivityTracker.currentActivity
            val result = JSONObject()

            if (activity is FragmentActivity) {
                val fragment = findVisibleLeafFragment(activity)
                result.put("screen", fragment?.javaClass?.simpleName ?: "none")
                val links = extractLinks(fragment)
                result.put("count", links.length())
                result.put("links", links)
            } else {
                result.put("error", "No FragmentActivity in foreground")
            }
            result
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleNavigate(uri: String): Response {
        val tab = uri.removePrefix("/navigate/")
        if (tab.isBlank()) {
            return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().apply {
                    put("error", "Missing tab name")
                    put(
                        "available_tabs",
                        JSONArray(AVAILABLE_TABS),
                    )
                },
            )
        }
        val json = runOnMainThread {
            DebugStateHelper.switchTab(appContext, tab)
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleVoteEntry(uri: String, vote: Boolean): Response {
        val idStr = uri.substringAfterLast("/")
        val entryId = idStr.toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid entry id: $idStr"),
            )

        val action = if (vote) "vote_entry" else "unvote_entry"
        val json = runOnMainThreadSuspend {
            try {
                val response = if (vote) {
                    entriesApi.voteEntry(entryId)
                } else {
                    entriesApi.unvoteEntry(entryId)
                }

                JSONObject().apply {
                    put("action", action)
                    put("entry_id", entryId)
                    put("success", response.isSuccessful)
                    put("status_code", response.code())
                    if (!response.isSuccessful) {
                        put("error", response.message())
                    }
                }
            } catch (e: Exception) {
                Napier.w("Failed to $action for entry $entryId", e, tag = TAG)
                JSONObject().apply {
                    put("action", action)
                    put("entry_id", entryId)
                    put("success", false)
                    put("error", e.message ?: e.javaClass.simpleName)
                }
            }
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleListRelatedLinks(uri: String): Response {
        val idStr = uri.substringAfterLast("/")
        val linkId = idStr.toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid link id: $idStr"),
            )

        val json = runOnMainThreadSuspend {
            try {
                val response = linksApi.getRelated(linkId = linkId)
                val items = response.data.orEmpty()
                JSONObject().apply {
                    put("link_id", linkId)
                    put("count", items.size)
                    put("related", JSONArray().apply {
                        for (item in items) {
                            put(JSONObject().apply {
                                put("id", item.id)
                                put("title", item.title)
                                put("url", item.url)
                                put("votes_up", item.votes.up)
                                put("votes_down", item.votes.down)
                                put("voted", item.voted)
                                put("author", item.author.username)
                            })
                        }
                    })
                }
            } catch (e: java.io.IOException) {
                Napier.w("Failed to list related links for link $linkId", e, tag = TAG)
                JSONObject().apply {
                    put("error", e.message ?: e.javaClass.simpleName)
                }
            } catch (e: retrofit2.HttpException) {
                Napier.w("Failed to list related links for link $linkId", e, tag = TAG)
                JSONObject().apply {
                    put("error", e.message ?: e.javaClass.simpleName)
                }
            }
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleOpenRelatedLinks(uri: String): Response {
        val idStr = uri.substringAfterLast("/")
        val linkId = idStr.toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid link id: $idStr"),
            )

        val json = runOnMainThread {
            val intent = RelatedLinksActivity.createIntent(appContext, linkId)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            JSONObject().apply {
                put("action", "open_related_links")
                put("success", true)
                put("link_id", linkId)
            }
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleVoteRelated(uri: String): Response {
        // URI: /action/vote/related/{linkId}/{relatedId}/{up|down}
        val parts = uri.removePrefix("/action/vote/related/").split("/")
        if (parts.size != 3) {
            return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Expected /action/vote/related/{linkId}/{relatedId}/{up|down}"),
            )
        }
        val linkId = parts[0].toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid linkId: ${parts[0]}"),
            )
        val relatedId = parts[1].toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid relatedId: ${parts[1]}"),
            )
        val voteType = parts[2]

        val json = runOnMainThreadSuspend {
            try {
                val response = linksApi.voteRelated(
                    linkId = linkId,
                    relatedId = relatedId,
                    type = voteType,
                )
                JSONObject().apply {
                    put("action", "vote_related")
                    put("link_id", linkId)
                    put("related_id", relatedId)
                    put("vote_type", voteType)
                    put("success", response.isSuccessful)
                    put("status_code", response.code())
                    if (!response.isSuccessful) {
                        put("error", response.message())
                    }
                }
            } catch (e: java.io.IOException) {
                Napier.w("Failed to vote related link=$linkId related=$relatedId type=$voteType", e, tag = TAG)
                JSONObject().apply {
                    put("action", "vote_related")
                    put("link_id", linkId)
                    put("related_id", relatedId)
                    put("success", false)
                    put("error", e.message ?: e.javaClass.simpleName)
                }
            } catch (e: retrofit2.HttpException) {
                Napier.w("Failed to vote related link=$linkId related=$relatedId type=$voteType", e, tag = TAG)
                JSONObject().apply {
                    put("action", "vote_related")
                    put("link_id", linkId)
                    put("related_id", relatedId)
                    put("success", false)
                    put("error", e.message ?: e.javaClass.simpleName)
                }
            }
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleOpenLink(uri: String): Response {
        val idStr = uri.substringAfterLast("/")
        val linkId = idStr.toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid link id: $idStr"),
            )

        val json = runOnMainThread {
            DebugStateHelper.openLink(appContext, linkId)
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleOpenEntry(uri: String): Response {
        val idStr = uri.substringAfterLast("/")
        val entryId = idStr.toLongOrNull()
            ?: return jsonResponse(
                Response.Status.BAD_REQUEST,
                JSONObject().put("error", "Invalid entry id: $idStr"),
            )

        val json = runOnMainThread {
            DebugStateHelper.openEntry(appContext, entryId)
        }
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleClearCache(): Response {
        val json = DebugStateHelper.clearCache(appContext)
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleLogout(): Response {
        val json = runOnMainThreadSuspend {
            DebugStateHelper.forceLogout(appContext)
        }
        return jsonResponse(Response.Status.OK, json)
    }

    // --- Diagnostic checkpoints ---

    private fun handleCheckpoints(session: IHTTPSession): Response {
        val filterTag = session.parms?.get("tag")
        val json = DiagnosticCheckpoint.toJson(filterTag)
        return jsonResponse(Response.Status.OK, json)
    }

    private fun handleClearCheckpoints(): Response {
        val json = DiagnosticCheckpoint.clear()
        return jsonResponse(Response.Status.OK, json)
    }

    // --- UI traversal helpers (must run on main thread) ---

    private fun findVisibleLeafFragment(activity: FragmentActivity): Fragment? {
        val topFragment = activity.supportFragmentManager.fragments
            .firstOrNull { it.isVisible }
            ?: return null

        val childFragment = topFragment.childFragmentManager.fragments
            .firstOrNull { it.isVisible }

        return childFragment ?: topFragment
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractEntries(fragment: Fragment?): JSONArray {
        val result = JSONArray()
        if (fragment == null) return result

        val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.recyclerView)
            ?: return result
        val adapter = recyclerView.adapter ?: return result

        if (adapter is EndlessProgressAdapter<*, *>) {
            for (item in adapter.data) {
                when (item) {
                    is Entry -> result.put(entryToJson(item))
                    is EntryLink -> {
                        item.entry?.let { result.put(entryToJson(it)) }
                    }
                }
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractLinks(fragment: Fragment?): JSONArray {
        val result = JSONArray()
        if (fragment == null) return result

        val recyclerView = fragment.view?.findViewById<RecyclerView>(R.id.recyclerView)
            ?: return result
        val adapter = recyclerView.adapter ?: return result

        if (adapter is EndlessProgressAdapter<*, *>) {
            for (item in adapter.data) {
                when (item) {
                    is Link -> result.put(linkToJson(item))
                    is EntryLink -> {
                        item.link?.let { result.put(linkToJson(it)) }
                    }
                }
            }
        }
        return result
    }

    private fun entryToJson(entry: Entry): JSONObject = JSONObject().apply {
        put("id", entry.id)
        put("body", entry.body)
        put("author", entry.author.nick)
        put("vote_count", entry.voteCount)
        put("is_voted", entry.isVoted)
        put("comments_count", entry.commentsCount)
    }

    private fun linkToJson(link: Link): JSONObject = JSONObject().apply {
        put("id", link.id)
        put("title", link.title)
        put("url", link.sourceUrl)
        put("vote_count", link.voteCount)
        put("comments_count", link.commentsCount)
    }

    private fun guessItemType(adapter: RecyclerView.Adapter<*>): String =
        when {
            adapter.javaClass.simpleName.contains("Entry", ignoreCase = true) -> "entry"
            adapter.javaClass.simpleName.contains("Link", ignoreCase = true) -> "link"
            else -> "unknown"
        }

    // --- Thread bridge ---

    /**
     * Like [runOnMainThread] but for suspend functions (e.g. logout).
     */
    private fun runOnMainThreadSuspend(block: suspend () -> JSONObject): JSONObject {
        val latch = CountDownLatch(1)
        var result = JSONObject()
        Handler(Looper.getMainLooper()).post {
            kotlinx.coroutines.MainScope().launch {
                try {
                    result = block()
                } catch (e: Exception) {
                    result = JSONObject().apply {
                        put("error", e.message ?: e.javaClass.simpleName)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        if (!latch.await(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return JSONObject().put("error", "Main thread timeout (${MAIN_THREAD_TIMEOUT_SECONDS}s)")
        }
        return result
    }

    /**
     * Execute [block] on the main thread and wait for result.
     * NanoHTTPD calls serve() on worker threads; UI access requires main thread.
     */
    private fun runOnMainThread(block: () -> JSONObject): JSONObject {
        val latch = CountDownLatch(1)
        var result = JSONObject()
        Handler(Looper.getMainLooper()).post {
            try {
                result = block()
            } catch (e: Exception) {
                result = JSONObject().apply {
                    put("error", e.message ?: e.javaClass.simpleName)
                }
            } finally {
                latch.countDown()
            }
        }
        if (!latch.await(MAIN_THREAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return JSONObject().put("error", "Main thread timeout (${MAIN_THREAD_TIMEOUT_SECONDS}s)")
        }
        return result
    }

    private fun jsonResponse(status: Response.Status, json: JSONObject): Response =
        newFixedLengthResponse(status, "application/json", json.toString(2))

    private fun endpoint(method: String, path: String, description: String): JSONObject =
        JSONObject().apply {
            put("method", method)
            put("path", path)
            put("description", description)
        }

}

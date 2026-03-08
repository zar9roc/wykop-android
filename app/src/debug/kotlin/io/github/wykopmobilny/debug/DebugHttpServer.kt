package io.github.wykopmobilny.debug

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.fragment.app.FragmentActivity
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URLDecoder

/**
 * Lightweight debug HTTP server running on port 8099.
 * Exposes app state and debug actions via REST API.
 *
 * Endpoints:
 *   GET  /           — HTML dashboard
 *   GET  /state      — app state JSON (add ?verbose=true for details)
 *   POST /clear-cache — clear app cache
 *   POST /logout      — force logout
 *   POST /switch-tab?tab=promoted — switch tab
 *
 * Access: adb forward tcp:8099 tcp:8099 && curl http://localhost:8099/state
 */
class DebugHttpServer(
    private val context: Context,
) {
    companion object {
        private const val TAG = "DebugHttpServer"
        private const val PORT = 8099
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var running = false

    fun start() {
        if (running) return
        running = true
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Napier.i("Debug HTTP server started on port $PORT", tag = TAG)
                while (running) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        launch {
                            try {
                                handleClient(socket)
                            } catch (e: IOException) {
                                Napier.w("Error handling client", e, tag = TAG)
                            } finally {
                                try {
                                    socket.close()
                                } catch (_: IOException) {
                                    // ignore
                                }
                            }
                        }
                    } catch (e: IOException) {
                        if (running) {
                            Napier.w("Error accepting connection", e, tag = TAG)
                        }
                    }
                }
            } catch (e: IOException) {
                Napier.w("Failed to start debug HTTP server", e, tag = TAG)
            }
        }
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (_: IOException) {
            // ignore
        }
        serverSocket = null
        Napier.i("Debug HTTP server stopped", tag = TAG)
    }

    private suspend fun handleClient(socket: java.net.Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(socket.getOutputStream(), true)

        val requestLine = input.readLine() ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 2) return

        val method = parts[0]
        val fullPath = parts[1]
        val pathAndQuery = fullPath.split("?", limit = 2)
        val path = pathAndQuery[0]
        val queryParams = parseQueryParams(pathAndQuery.getOrNull(1))

        val response =
            when {
                method == "GET" && path == "/" -> dashboard()

                method == "GET" && path == "/state" -> state(queryParams)

                method == "POST" && path == "/clear-cache" -> clearCache()

                method == "POST" && path == "/logout" -> logout()

                method == "POST" && path == "/switch-tab" -> switchTab(queryParams)

                // Allow GET for actions too (convenience from browser)
                method == "GET" && path == "/clear-cache" -> clearCache()

                method == "GET" && path == "/logout" -> logout()

                method == "GET" && path == "/switch-tab" -> switchTab(queryParams)

                else -> HttpResponse(404, "application/json", """{"error":"Not found","path":"$path"}""")
            }

        output.print("HTTP/1.1 ${response.statusCode} ${response.statusText}\r\n")
        output.print("Content-Type: ${response.contentType}; charset=utf-8\r\n")
        output.print("Access-Control-Allow-Origin: *\r\n")
        output.print("Connection: close\r\n")
        output.print("\r\n")
        output.print(response.body)
        output.flush()
    }

    private fun parseQueryParams(query: String?): Map<String, String> {
        if (query.isNullOrEmpty()) return emptyMap()
        return query
            .split("&")
            .mapNotNull { param ->
                val kv = param.split("=", limit = 2)
                if (kv.size == 2) {
                    URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv[1], "UTF-8")
                } else {
                    URLDecoder.decode(kv[0], "UTF-8") to ""
                }
            }.toMap()
    }

    // --- Endpoints ---

    private fun state(params: Map<String, String>): HttpResponse {
        val verbose = params["verbose"]?.toBoolean() ?: false
        val json = buildState(verbose)
        return HttpResponse(200, "application/json", json.toString(2))
    }

    private suspend fun clearCache(): HttpResponse =
        try {
            val cacheDir = context.cacheDir
            val filesDeleted = deleteRecursive(cacheDir)
            val result =
                JSONObject().apply {
                    put("action", "clear_cache")
                    put("success", true)
                    put("files_deleted", filesDeleted)
                    put("cache_path", cacheDir.absolutePath)
                }
            HttpResponse(200, "application/json", result.toString(2))
        } catch (e: IOException) {
            errorResponse("clear_cache", e.message)
        }

    private suspend fun logout(): HttpResponse {
        return try {
            val app =
                context.applicationContext as? WykopApp
                    ?: return errorResponse("logout", "WykopApp not available")

            val userManager = app.userManagerApi.get()
            val wasLoggedIn = userManager.isUserAuthorized()
            withContext(Dispatchers.Main) {
                userManager.logoutUser()
            }
            val result =
                JSONObject().apply {
                    put("action", "logout")
                    put("success", true)
                    put("was_logged_in", wasLoggedIn)
                }
            HttpResponse(200, "application/json", result.toString(2))
        } catch (e: IOException) {
            errorResponse("logout", e.message)
        }
    }

    private suspend fun switchTab(params: Map<String, String>): HttpResponse {
        val tab =
            params["tab"]
                ?: return HttpResponse(
                    400,
                    "application/json",
                    """{"error":"Missing 'tab' parameter","available_tabs":["promoted","upcoming","hits","hot","mywykop","favorite","search","messages","notifications"]}""",
                )

        return try {
            withContext(Dispatchers.Main) {
                val intent =
                    Intent(context, MainNavigationActivity::class.java).apply {
                        putExtra("debug_target_tab", tab)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                context.startActivity(intent)
            }
            val result =
                JSONObject().apply {
                    put("action", "switch_tab")
                    put("success", true)
                    put("tab", tab)
                }
            HttpResponse(200, "application/json", result.toString(2))
        } catch (e: IOException) {
            errorResponse("switch_tab", e.message)
        }
    }

    private fun dashboard(): HttpResponse {
        val html =
            buildString {
                append("<!DOCTYPE html><html><head>")
                append("<meta charset='utf-8'>")
                append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                append("<title>Wykop Debug</title>")
                append("<style>")
                append(
                    "body{font-family:system-ui,sans-serif;max-width:600px;margin:20px auto;padding:0 16px;background:#1a1a2e;color:#e0e0e0}",
                )
                append("h1{color:#e94560}h2{color:#0f3460;margin-top:24px}")
                append("a{color:#e94560;text-decoration:none}")
                append("a:hover{text-decoration:underline}")
                append(".endpoint{background:#16213e;border-radius:8px;padding:12px 16px;margin:8px 0}")
                append(".method{font-weight:bold;color:#0f3460;margin-right:8px}")
                append(".get{color:#53cf6c}.post{color:#e9a545}")
                append("button{background:#e94560;color:white;border:none;padding:8px 16px;border-radius:4px;cursor:pointer;margin:4px}")
                append("button:hover{background:#c73450}")
                append(
                    "#result{background:#16213e;border-radius:8px;padding:12px;margin-top:16px;white-space:pre-wrap;font-family:monospace;font-size:13px;display:none}",
                )
                append("</style></head><body>")
                append("<h1>Wykop Debug Server</h1>")
                append("<p>Port $PORT | Debug build only</p>")

                append("<h2>Endpoints</h2>")
                append("<div class='endpoint'><span class='method get'>GET</span><a href='/state'>/state</a> — stan aplikacji</div>")
                append(
                    "<div class='endpoint'><span class='method get'>GET</span><a href='/state?verbose=true'>/state?verbose=true</a> — stan szczegółowy</div>",
                )
                append("<div class='endpoint'><span class='method post'>POST</span>/clear-cache — czyszczenie cache</div>")
                append("<div class='endpoint'><span class='method post'>POST</span>/logout — wylogowanie</div>")
                append("<div class='endpoint'><span class='method post'>POST</span>/switch-tab?tab=... — zmiana zakładki</div>")

                append("<h2>Akcje</h2>")
                append("<button onclick=\"doFetch('/state')\">Stan</button>")
                append("<button onclick=\"doFetch('/state?verbose=true')\">Stan (verbose)</button>")
                append("<button onclick=\"doFetch('/clear-cache',{method:'POST'})\">Wyczyść cache</button>")
                append("<button onclick=\"doFetch('/logout',{method:'POST'})\">Wyloguj</button>")

                append("<div style='margin-top:8px'>")
                val tabs = listOf("promoted", "upcoming", "hits", "hot", "mywykop", "favorite")
                for (tab in tabs) {
                    append("<button onclick=\"doFetch('/switch-tab?tab=$tab',{method:'POST'})\">$tab</button>")
                }
                append("</div>")

                append("<div id='result'></div>")
                append("<script>")
                append(
                    "async function doFetch(url,opts){const r=document.getElementById('result');r.style.display='block';r.textContent='Loading...';",
                )
                append(
                    "try{const res=await fetch(url,opts);const json=await res.json();r.textContent=JSON.stringify(json,null,2)}catch(e){r.textContent='Error: '+e.message}}",
                )
                append("</script>")
                append("</body></html>")
            }
        return HttpResponse(200, "text/html", html)
    }

    // --- Helpers ---

    private fun buildState(verbose: Boolean): JSONObject {
        val json = JSONObject()
        val activity = DebugActivityTracker.currentActivity

        json.put("activity", activity?.javaClass?.simpleName ?: "none (app in background)")
        json.put("activity_class", activity?.javaClass?.name ?: JSONObject.NULL)

        if (activity is FragmentActivity) {
            val fm = activity.supportFragmentManager
            val fragments = fm.fragments
            val visibleFragments = fragments.filter { it.isVisible }

            json.put("fragment", visibleFragments.firstOrNull()?.javaClass?.simpleName ?: "none")
            json.put(
                "all_fragments",
                JSONArray(
                    fragments.map { f ->
                        JSONObject().apply {
                            put("name", f.javaClass.simpleName)
                            put("visible", f.isVisible)
                            put("resumed", f.isResumed)
                        }
                    },
                ),
            )

            if (verbose) {
                json.put("back_stack_count", fm.backStackEntryCount)
                val backStack = JSONArray()
                for (i in 0 until fm.backStackEntryCount) {
                    backStack.put(fm.getBackStackEntryAt(i).name ?: "unnamed")
                }
                json.put("back_stack", backStack)
            }

            visibleFragments.firstOrNull()?.let { topFragment ->
                val childFragments = topFragment.childFragmentManager.fragments
                if (childFragments.isNotEmpty()) {
                    val visibleChildren = childFragments.filter { it.isVisible }
                    json.put(
                        "child_fragment",
                        visibleChildren.firstOrNull()?.javaClass?.simpleName ?: "none",
                    )
                    json.put(
                        "child_fragments",
                        JSONArray(
                            childFragments.map { f ->
                                JSONObject().apply {
                                    put("name", f.javaClass.simpleName)
                                    put("visible", f.isVisible)
                                }
                            },
                        ),
                    )
                }
            }
        }

        val app = context.applicationContext as? WykopApp
        if (app != null) {
            val userManager = app.userManagerApi.get()
            val credentials = userManager.getUserCredentials()
            json.put("user_logged_in", userManager.isUserAuthorized())
            json.put("user_login", credentials?.login ?: JSONObject.NULL)
        }

        json.put("package", context.packageName)
        json.put("version_name", getVersionName())
        json.put("server_port", PORT)

        if (verbose) {
            json.put(
                "device",
                JSONObject().apply {
                    put("model", Build.MODEL)
                    put("sdk", Build.VERSION.SDK_INT)
                    put("manufacturer", Build.MANUFACTURER)
                },
            )
        }

        return json
    }

    private fun getVersionName(): String =
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }

    private fun deleteRecursive(fileOrDirectory: java.io.File): Int {
        var count = 0
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.let { files ->
                for (child in files) {
                    count += deleteRecursive(child)
                }
            }
        }
        if (fileOrDirectory.delete()) {
            count++
        }
        return count
    }

    private fun errorResponse(
        action: String,
        message: String?,
    ): HttpResponse {
        val json =
            JSONObject().apply {
                put("action", action)
                put("success", false)
                put("error", message ?: "Unknown error")
            }
        return HttpResponse(500, "application/json", json.toString(2))
    }

    private data class HttpResponse(
        val statusCode: Int,
        val contentType: String,
        val body: String,
    ) {
        val statusText: String
            get() =
                when (statusCode) {
                    200 -> "OK"
                    400 -> "Bad Request"
                    404 -> "Not Found"
                    500 -> "Internal Server Error"
                    else -> "Unknown"
                }
    }
}

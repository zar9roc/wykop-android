package io.github.wykopmobilny.glide

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Postęp pobierania obrazków ładowanych przez Glide.
 *
 * Glide nie wystawia postępu sieci, więc podpinamy się interceptorem do jego
 * klienta OkHttp: body odpowiedzi jest opakowywane w licznik bajtów, a słuchacz
 * zarejestrowany pod adresem URL dostaje (pobrane, całkowite) bajty.
 * Całkowity rozmiar bywa nieznany (-1), gdy serwer nie zwraca Content-Length.
 *
 * Wywołania przychodzą z wątku sieciowego - słuchacz musi sam przeskoczyć na main.
 */
object GlideProgressSupport {
    fun interface Listener {
        fun onProgress(
            bytesRead: Long,
            totalBytes: Long,
        )
    }

    private val listeners = ConcurrentHashMap<String, Listener>()

    fun register(
        url: String,
        listener: Listener,
    ) {
        listeners[url] = listener
    }

    fun unregister(url: String) {
        listeners.remove(url)
    }

    val interceptor =
        Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val body = response.body
            val listener = listeners[request.url.toString()]
            if (body == null || listener == null) {
                response
            } else {
                response
                    .newBuilder()
                    .body(ProgressResponseBody(body, listener))
                    .build()
            }
        }

    private class ProgressResponseBody(
        private val delegate: ResponseBody,
        private val listener: Listener,
    ) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? = delegate.contentType()

        override fun contentLength(): Long = delegate.contentLength()

        override fun source(): BufferedSource =
            bufferedSource ?: counting(delegate.source()).buffer().also { bufferedSource = it }

        private fun counting(source: Source): Source {
            // ForwardingSource ma wlasne pole `delegate` przeslaniajace pole klasy.
            val contentLength = delegate.contentLength()
            return object : ForwardingSource(source) {
                private var totalRead = 0L

                override fun read(
                    sink: Buffer,
                    byteCount: Long,
                ): Long {
                    val read = super.read(sink, byteCount)
                    if (read > 0) {
                        totalRead += read
                        listener.onProgress(totalRead, contentLength)
                    }
                    return read
                }
            }
        }
    }
}

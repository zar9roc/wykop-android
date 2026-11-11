package io.github.wykopmobilny.ui.modules.photoview

import java.io.File

/**
 * Naprawa uszkodzonych (uucietych) GIF-ow z CDN Wykopu.
 *
 * CDN potrafi serwowac plik obciety w polowie ostatniej klatki - bez terminatora 0x3B
 * (np. wpis 87995201: 37 pelnych klatek + urwana 38., Content-Length zgodny z obcieta
 * zawartoscia). Przegladarki graja to, co da sie odczytac, a GifHeaderParser Glide'a
 * konczy sie STATUS_FORMAT_ERROR -> GifDrawable odpada, Glide spada do statycznej
 * bitmapy i na ekranie zostaje czarna pierwsza klatka.
 *
 * Obcinamy plik do konca ostatniej kompletnej klatki i dopisujemy terminator.
 */
internal object GifRepair {
    private const val REPAIRED_DIR = "gif_repaired"
    private const val STALE_AFTER_MS = 60 * 60 * 1000L

    /** Zwraca [file] gdy jest poprawny (lub nie do uratowania), inaczej naprawiona kopie w [cacheDir]. */
    fun repairIfTruncated(
        file: File,
        cacheDir: File,
    ): File {
        val bytes = file.readBytes()
        val lastCompleteFrameEnd = findTruncation(bytes) ?: return file
        val dir = File(cacheDir, REPAIRED_DIR).apply { mkdirs() }
        val now = System.currentTimeMillis()
        dir.listFiles()?.filter { now - it.lastModified() > STALE_AFTER_MS }?.forEach(File::delete)
        val repaired = File(dir, "${file.name}.gif")
        repaired.outputStream().use { out ->
            out.write(bytes, 0, lastCompleteFrameEnd)
            out.write(TRAILER)
        }
        return repaired
    }

    /**
     * null = plik kompletny (ma terminator) albo nie ma ani jednej pelnej klatki;
     * inaczej offset konca ostatniej kompletnej klatki.
     */
    internal fun findTruncation(b: ByteArray): Int? {
        if (b.size < HEADER_SIZE || String(b, 0, 3, Charsets.US_ASCII) != "GIF") return null
        val screenFlags = b.u8(10)
        var p = HEADER_SIZE
        if (screenFlags and COLOR_TABLE_FLAG != 0) p += colorTableSize(screenFlags)
        var lastCompleteFrameEnd = -1
        while (true) {
            if (p >= b.size) break
            when (b.u8(p++)) {
                EXTENSION -> {
                    p++ // etykieta
                    p = b.skipSubBlocks(p) ?: break
                }

                IMAGE -> {
                    if (p + IMAGE_DESCRIPTOR_SIZE > b.size) break
                    val flags = b.u8(p + 8)
                    p += IMAGE_DESCRIPTOR_SIZE
                    if (flags and COLOR_TABLE_FLAG != 0) p += colorTableSize(flags)
                    p++ // minimalny rozmiar kodu LZW
                    p = b.skipSubBlocks(p) ?: break
                    lastCompleteFrameEnd = p
                }

                TRAILER -> return null

                else -> break // smieci po klatkach - traktujemy jak urwanie
            }
        }
        return lastCompleteFrameEnd.takeIf { it > 0 }
    }

    /** Pozycja za terminatorem pod-blokow albo null gdy dane sie urwaly. */
    private fun ByteArray.skipSubBlocks(start: Int): Int? {
        var p = start
        while (true) {
            if (p >= size) return null
            val len = u8(p)
            p += 1 + len
            if (len == 0) return p
        }
    }

    private fun ByteArray.u8(index: Int) = this[index].toInt() and 0xFF

    private fun colorTableSize(flags: Int) = 3 * (2 shl (flags and 0x07))

    private const val HEADER_SIZE = 13
    private const val IMAGE_DESCRIPTOR_SIZE = 9
    private const val COLOR_TABLE_FLAG = 0x80
    private const val EXTENSION = 0x21
    private const val IMAGE = 0x2C
    private const val TRAILER = 0x3B
}

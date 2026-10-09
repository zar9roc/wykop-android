package io.github.wykopmobilny.api

/** Zdjecie zalaczone do tresci: plik z urzadzenia albo adres obrazka (wpisany lub z edycji). */
sealed interface PhotoSource {
    class File(
        val file: WykopImageFile,
    ) : PhotoSource

    class Url(
        val url: String,
    ) : PhotoSource
}

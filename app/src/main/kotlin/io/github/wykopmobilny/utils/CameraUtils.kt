package io.github.wykopmobilny.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import io.github.aakira.napier.Napier
import java.io.File
import java.io.IOException

object CameraUtils {
    fun createPictureUri(context: Context): Uri? {
        val filename = "owmcamera_${System.currentTimeMillis()}.jpg"
        // Katalog aplikacji zamiast publicznego Pictures - zapis do publicznego
        // katalogu wymaga uprawnien niedostepnych od Androida 10 (scoped storage),
        // a zdjecie i tak jest tylko buforem do wyslania na serwer.
        val file =
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                filename,
            )

        try {
            file.parentFile?.mkdirs()
            file.createNewFile()
        } catch (exception: IOException) {
            Napier.w("Couldn't create file", exception)
        }

        return FileProvider.getUriForFile(context, context.applicationContext.packageName + ".fileprovider", file)
    }
}

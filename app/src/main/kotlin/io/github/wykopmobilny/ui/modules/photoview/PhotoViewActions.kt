package io.github.wykopmobilny.ui.modules.photoview

import android.Manifest
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore.Images
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.base.WykopSchedulers
import io.reactivex.Completable
import io.reactivex.Single
import java.io.File
import java.io.IOException
import java.nio.file.Files

interface PhotoViewCallbacks {
    fun shareImage(url: String)

    fun getDrawable(): Drawable?

    /** Zapis do Pictures/wykopmobilny; nazwa z [label] (gdy jest), przy duplikacie z indeksem. */
    fun saveImage(
        url: String,
        label: String? = null,
    )
}

class PhotoViewActions(
    val context: Context,
) : PhotoViewCallbacks {
    companion object {
        const val SAVED_FOLDER = "wykopmobilny"
        private const val SHARE_CACHE_DIR = "shared_media"
    }

    private val photoView = context as PhotoViewActivity

    override fun shareImage(url: String) {
        // Plik do udostepnienia ladujemy w cache aplikacji (cache-path w provider_paths):
        // bez uprawnien do pamieci i bez zasmiecania galerii. Dawniej kopia szla do
        // Pictures/wykopmobilny/udostepnione pod nazwa url.substringAfterLast("/") - z query
        // stringiem CDN ("x.jpg?author=..&auth=..") zapis padal (MediaProvider odrzuca '?'
        // w nazwie), a blad byl tylko logowany - przycisk "nic nie robil".
        Single
            .fromCallable {
                val source =
                    Glide
                        .with(context)
                        .downloadOnly()
                        .load(url)
                        .submit()
                        .get()
                val dir = File(context.cacheDir, SHARE_CACHE_DIR)
                // Poprzednio udostepnione pliki - odbiorca dawno je przeczytal.
                dir.deleteRecursively()
                dir.mkdirs()
                File(dir, savedFileName(url, null).replace(FORBIDDEN_FILE_NAME_CHARS, "_").ifEmpty { "image" })
                    .also { source.copyTo(it, overwrite = true) }
            }.subscribeOn(WykopSchedulers().backgroundThread())
            .observeOn(WykopSchedulers().mainThread())
            .subscribe(
                { file ->
                    val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".fileprovider", file)
                    val share =
                        Intent(Intent.ACTION_SEND).apply {
                            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            // ClipData - zeby uprawnienie do odczytu przeszlo przez chooser na wybrana aplikacje.
                            clipData = ClipData.newRawUri(null, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    photoView.startActivity(Intent.createChooser(share, "Udostępnij obrazek"))
                },
                {
                    if (it is IOException) {
                        Napier.i("Failed to share image $url", throwable = it)
                    } else {
                        Napier.e("Failed to share image $url", throwable = it)
                    }
                    showToastMessage("Nie udało się udostępnić obrazka")
                },
            )
    }

    override fun getDrawable(): Drawable? {
        /*photoView.image.drawable?.apply {
            return this
        }*/
        return null
    }

    override fun saveImage(
        url: String,
        label: String?,
    ) {
        if (!checkForWriteReadPermission()) {
            return
        }
        saveImageV2(url, label)
            .subscribeOn(WykopSchedulers().backgroundThread())
            .observeOn(WykopSchedulers().mainThread())
            .subscribe(
                { showToastMessage("Zapisano plik") },
                { showToastMessage("Błąd podczas zapisu pliku") },
            )
    }

    @Suppress("DEPRECATION")
    private fun saveImageV2(
        url: String,
        label: String?,
    ): Completable =
        Completable.fromAction {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val source =
                    Glide
                        .with(context)
                        .downloadOnly()
                        .load(url)
                        .submit()
                        .get()
                val fileName =
                    uniqueFileName(savedFileName(url, label)) { candidate ->
                        existsInMediaStore(candidate, Environment.DIRECTORY_PICTURES + File.separator + SAVED_FOLDER + File.separator)
                    }
                val values =
                    ContentValues().apply {
                        put(Images.Media.DISPLAY_NAME, fileName)
                        put(Images.Media.MIME_TYPE, getMimeType(url))
                        put(Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + SAVED_FOLDER)
                        put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
                    }

                val uri = context.contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values).let(::checkNotNull)
                context.contentResolver.openOutputStream(uri)?.use { Files.copy(source.toPath(), it) }
            } else {
                val source =
                    Glide
                        .with(context)
                        .downloadOnly()
                        .load(url)
                        .submit()
                        .get()
                val directory =
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        SAVED_FOLDER,
                    )
                val fileName = uniqueFileName(savedFileName(url, label)) { File(directory, it).exists() }
                val targetFile = File(directory, fileName)
                source.copyTo(targetFile, false)
                addImageToGallery(targetFile.path, context)
            }
        }

    // MediaStore widzi tu pliki zapisane przez aplikacje - to wystarczy, zeby kolejne
    // zapisy tej samej etykiety dostawaly indeks zamiast nadpisywac/duplikowac nazwe.
    private fun existsInMediaStore(
        fileName: String,
        relativePath: String,
    ): Boolean =
        context.contentResolver
            .query(
                Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(Images.Media._ID),
                "${Images.Media.DISPLAY_NAME} = ? AND ${Images.Media.RELATIVE_PATH} = ?",
                arrayOf(fileName, relativePath),
                null,
            )?.use { it.count > 0 } ?: false

    private fun checkForWriteReadPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // We don't need these permissions for saving images on Android API >= T
            return true
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        ActivityCompat.requestPermissions(
            photoView,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
            1,
        )
        val writePermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        val readPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        return writePermission == PackageManager.PERMISSION_GRANTED && readPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun showToastMessage(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    private fun addImageToGallery(
        filePath: String,
        context: Context,
    ) {
        // Rejestracja istniejącego pliku w galerii przez MediaScanner. Insert do
        // MediaStore z kolumną _data rzuca na Androidzie 10+ "Mutation of _data
        // is not allowed" (crash z logów 2026-07-16).
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null, null)
    }

    private fun getMimeType(uri: String) = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri))
}

private val FORBIDDEN_FILE_NAME_CHARS = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
private const val MAX_FILE_NAME_LENGTH = 100

/**
 * Nazwa pliku do zapisu: etykieta zdjecia z rozszerzeniem z adresu, a bez etykiety
 * nazwa pliku z CDN. Znaki niedozwolone w nazwach plikow zamieniane na "_".
 */
internal fun savedFileName(
    url: String,
    label: String?,
): String {
    val urlName = url.substringBefore('?').substringAfterLast('/')
    val extension = urlName.substringAfterLast('.', "").lowercase().takeIf { it.length in 1..4 }
    val base =
        label
            ?.replace(FORBIDDEN_FILE_NAME_CHARS, "_")
            ?.trim()
            ?.trimEnd('.')
            ?.take(MAX_FILE_NAME_LENGTH)
            ?.takeIf { it.isNotEmpty() }
            ?: return urlName
    // Etykieta czesto juz konczy sie rozszerzeniem ("zdjecie.jpg") - nie doklejamy drugiego.
    return if (extension == null || base.endsWith(".$extension", ignoreCase = true)) base else "$base.$extension"
}

/** "nazwa.jpg", a gdy zajeta: "nazwa (2).jpg", "nazwa (3).jpg"... */
internal fun uniqueFileName(
    fileName: String,
    exists: (String) -> Boolean,
): String {
    if (!exists(fileName)) return fileName
    val dot = fileName.lastIndexOf('.').takeIf { it > 0 }
    val base = dot?.let { fileName.substring(0, it) } ?: fileName
    val extension = dot?.let { fileName.substring(it) }.orEmpty()
    return generateSequence(2) { it + 1 }
        .map { "$base ($it)$extension" }
        .first { !exists(it) }
}

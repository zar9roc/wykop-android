package io.github.wykopmobilny.ui.modules.photoview

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.BaseActivity
import io.github.wykopmobilny.databinding.ActivityPhotoviewBinding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.glide.GlideProgressSupport
import io.github.wykopmobilny.utils.ClipboardHelperApi
import io.github.wykopmobilny.utils.viewBinding
import java.io.File
import javax.inject.Inject
import io.github.wykopmobilny.ui.base.android.R as BaseR

internal class PhotoViewActivity : BaseActivity() {
    companion object {
        const val URL_EXTRA = "URL"
        const val SHARE_REQUEST_CODE = 1

        fun createIntent(
            context: Context,
            imageUrl: String,
        ) = Intent(context, PhotoViewActivity::class.java).apply {
            putExtra(URL_EXTRA, imageUrl)
        }
    }

    @Inject
    lateinit var clipboardHelper: ClipboardHelperApi

    private val binding by viewBinding(ActivityPhotoviewBinding::inflate)

    override val enableSwipeBackLayout: Boolean = true // We manually attach it here
    override val isActivityTransfluent: Boolean = true

    lateinit var url: String
    private val photoViewActions: PhotoViewCallbacks by lazy { PhotoViewActions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar.toolbar)
        binding.toolbar.toolbar.setBackgroundResource(BaseR.drawable.gradient_toolbar_up)
        binding.loadingView.isIndeterminate = true
        title = null
        url = intent.getStringExtra(URL_EXTRA) ?: return finish()
        trackDownloadProgress()
        // CDN dokleja query string (?author=...&auth=...) - endsWith(".gif") na calym
        // URL-u kierowal gify do SubsamplingScaleImageView, ktory nie dekoduje GIF-ow
        // (pusty ekran). Rozszerzenie sprawdzamy na samej sciezce.
        if (Uri.parse(url).path.orEmpty().endsWith(".gif", ignoreCase = true)) {
            loadGif()
        } else {
            loadImage()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photoview_menu, menu)
        menu.findItem(R.id.action_save_mp4)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> photoViewActions.shareImage(url)
            R.id.action_save_image -> photoViewActions.saveImage(url)
            R.id.action_copy_url -> clipboardHelper.copyTextToClipboard(url, "imageUrl")
            R.id.action_open_browser -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun loadImage() {
        binding.image.isVisible = true
        binding.image.setMinimumDpi(70)
        binding.image.setMinimumTileDpi(240)
        binding.gif.isVisible = false
        Glide
            .with(this)
            .downloadOnly()
            .load(url)
            .into(
                object : CustomTarget<File>() {
                    override fun onResourceReady(
                        resource: File,
                        transition: Transition<in File>?,
                    ) {
                        hideProgress()
                        binding.image.setImage(ImageSource.uri(resource.absolutePath))
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                },
            )
    }

    private fun loadGif() {
        binding.image.isVisible = false
        binding.gif.isVisible = true
        // Najpierw pobieramy plik (postep na tym etapie), potem dekodujemy z dysku.
        // Dzieki temu znamy wymiary gifa i mozemy zdecydowac czy w ogole pomniejszac.
        Glide
            .with(this)
            .downloadOnly()
            .load(url)
            .into(
                object : CustomTarget<File>() {
                    override fun onResourceReady(
                        resource: File,
                        transition: Transition<in File>?,
                    ) {
                        hideProgress()
                        showGif(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        hideProgress()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                },
            )
    }

    private fun showGif(file: File) {
        // Wymiary pierwszej klatki - inJustDecodeBounds czyta tylko naglowek.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val exceedsTextureLimit = bounds.outWidth > MAX_GIF_DECODE_SIZE || bounds.outHeight > MAX_GIF_DECODE_SIZE
        Glide
            .with(this)
            .load(file)
            // KLUCZOWE dla plynnosci: transformacja (downsample/override) animowanego GIF-a
            // jest w Glide liczona DLA KAZDEJ KLATKI - przy wiekszych gifach dawalo to
            // odtwarzanie "klatka po klatce". Dlatego domyslnie NIE transformujemy - gif gra
            // natywnie i plynnie. Pomniejszamy tylko ekstremalne wymiary (np. 358x20000),
            // ktore przekraczaja limit tekstury GPU i inaczej daja pusty ekran.
            .let { request ->
                if (exceedsTextureLimit) {
                    request
                        .downsample(DownsampleStrategy.AT_MOST)
                        .override(MAX_GIF_DECODE_SIZE, MAX_GIF_DECODE_SIZE)
                } else {
                    request
                }
            }.into(binding.gif)
    }

    private fun trackDownloadProgress() {
        GlideProgressSupport.register(url) { bytesRead, totalBytes ->
            runOnUiThread {
                if (isDestroyed || !binding.loadingView.isVisible) return@runOnUiThread
                if (totalBytes > 0) {
                    binding.loadingView.isIndeterminate = false
                    binding.loadingView.progress =
                        (bytesRead * binding.loadingView.max / totalBytes).toInt()
                    binding.progressLabel.text = "${bytesRead.formatBytes()} / ${totalBytes.formatBytes()}"
                } else {
                    binding.progressLabel.text = bytesRead.formatBytes()
                }
                binding.progressLabel.isVisible = true
                DiagnosticCheckpoint.log("PhotoViewProgress", "${binding.progressLabel.text}")
            }
        }
    }

    private fun hideProgress() {
        binding.loadingView.isVisible = false
        binding.progressLabel.isVisible = false
    }

    override fun onDestroy() {
        GlideProgressSupport.unregister(url)
        super.onDestroy()
    }

    private fun Long.formatBytes(): String =
        if (this < BYTES_IN_MEGABYTE) {
            "${this / BYTES_IN_KILOBYTE} KB"
        } else {
            String.format(java.util.Locale.getDefault(), "%.1f MB", toDouble() / BYTES_IN_MEGABYTE)
        }
}

// Bezpieczny limit boku dekodowanego gifa - ponizej typowych limitow tekstur GPU (>=4096).
private const val MAX_GIF_DECODE_SIZE = 4096
private const val BYTES_IN_KILOBYTE = 1024L
private const val BYTES_IN_MEGABYTE = 1024L * 1024L

package io.github.wykopmobilny.ui.modules.photoview

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import io.github.wykopmobilny.databinding.ItemPhotoviewPageBinding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.glide.GlideProgressSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Strony przegladarki zdjec - jedna na adres, logika ladowania jak dawniej w PhotoViewActivity. */
internal class PhotoPagesAdapter(
    private val activity: ComponentActivity,
    private val urls: List<String>,
    /** Pojedyncze tapniecie w strone - przelacza widocznosc toolbara/paskow systemowych. */
    private val onTap: () -> Unit,
) : RecyclerView.Adapter<PhotoPagesAdapter.PageHolder>() {
    override fun getItemCount() = urls.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ) = PageHolder(ItemPhotoviewPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(
        holder: PageHolder,
        position: Int,
    ) = holder.bind(urls[position])

    override fun onViewRecycled(holder: PageHolder) = holder.unbind()

    inner class PageHolder(
        private val binding: ItemPhotoviewPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var url: String? = null
        private var target: CustomTarget<File>? = null
        private var gifJob: Job? = null

        init {
            // Oba widoki zglaszaja klik dopiero po onSingleTapConfirmed, wiec
            // podwojne tapniecie (zoom) nie przelacza przy okazji toolbara.
            binding.image.setOnClickListener { onTap() }
            binding.gif.setOnClickListener { onTap() }
            binding.root.setOnClickListener { onTap() }
        }

        fun bind(url: String) {
            unbind()
            this.url = url
            binding.loadingView.isVisible = true
            binding.loadingView.isIndeterminate = true
            binding.progressLabel.isVisible = false
            trackDownloadProgress(url)
            // CDN dokleja query string (?author=...&auth=...) - endsWith(".gif") na calym
            // URL-u kierowal gify do SubsamplingScaleImageView, ktory nie dekoduje GIF-ow
            // (pusty ekran). Rozszerzenie sprawdzamy na samej sciezce.
            if (Uri.parse(url).path.orEmpty().endsWith(".gif", ignoreCase = true)) {
                loadGif(url)
            } else {
                loadImage(url)
            }
        }

        fun unbind() {
            url?.let(GlideProgressSupport::unregister)
            url = null
            gifJob?.cancel()
            gifJob = null
            target?.let { Glide.with(activity).clear(it) }
            target = null
            Glide.with(activity).clear(binding.gif)
            binding.image.recycle()
        }

        private fun loadImage(url: String) {
            binding.image.isVisible = true
            binding.image.setMinimumDpi(70)
            binding.image.setMinimumTileDpi(240)
            binding.gif.isVisible = false
            download(url) { file -> binding.image.setImage(ImageSource.uri(file.absolutePath)) }
        }

        private fun loadGif(url: String) {
            binding.image.isVisible = false
            binding.gif.isVisible = true
            // Najpierw pobieramy plik (postep na tym etapie), potem dekodujemy z dysku.
            // Dzieki temu znamy wymiary gifa i mozemy zdecydowac czy w ogole pomniejszac.
            download(url, ::showGif)
        }

        private fun download(
            url: String,
            onReady: (File) -> Unit,
        ) {
            val target =
                object : CustomTarget<File>() {
                    override fun onResourceReady(
                        resource: File,
                        transition: Transition<in File>?,
                    ) {
                        hideProgress()
                        onReady(resource)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        hideProgress()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                }
            this.target = target
            Glide
                .with(activity)
                .downloadOnly()
                .load(url)
                .into(target)
        }

        private fun showGif(downloaded: File) {
            val url = url ?: return
            gifJob?.cancel()
            gifJob =
                activity.lifecycleScope.launch {
                    val file =
                        withContext(Dispatchers.IO) {
                            runCatching { GifRepair.repairIfTruncated(downloaded, activity.cacheDir) }.getOrDefault(downloaded)
                        }
                    if (this@PageHolder.url != url) return@launch
                    if (file != downloaded) DiagnosticCheckpoint.log("PhotoViewGif", "repaired truncated gif $url")
                    decodeGif(file)
                }
        }

        private fun decodeGif(file: File) {
            // Wymiary pierwszej klatki - inJustDecodeBounds czyta tylko naglowek.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val exceedsTextureLimit = bounds.outWidth > MAX_GIF_DECODE_SIZE || bounds.outHeight > MAX_GIF_DECODE_SIZE
            Glide
                .with(activity)
                .load(file)
                // Glide domyslnie zapetla GIF-y w nieskonczonosc. Bez rozszerzenia NETSCAPE2.0
                // GIF ma sie odegrac RAZ i zatrzymac na ostatniej klatce (tak robia przegladarki;
                // np. wpis 61875773 konczy sie biala klatka). LOOP_INTRINSIC = liczba petli z pliku.
                .listener(
                    object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean,
                        ) = false

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean,
                        ): Boolean {
                            (resource as? GifDrawable)?.setLoopCount(GifDrawable.LOOP_INTRINSIC)
                            return false
                        }
                    },
                )
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

        private fun trackDownloadProgress(url: String) {
            GlideProgressSupport.register(url) { bytesRead, totalBytes ->
                activity.runOnUiThread {
                    if (activity.isDestroyed || this.url != url || !binding.loadingView.isVisible) return@runOnUiThread
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

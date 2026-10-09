package io.github.wykopmobilny.ui.modules.photoview

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.viewpager2.widget.ViewPager2
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.BaseActivity
import io.github.wykopmobilny.databinding.ActivityPhotoviewBinding
import io.github.wykopmobilny.utils.ClipboardHelperApi
import io.github.wykopmobilny.utils.viewBinding
import javax.inject.Inject
import io.github.wykopmobilny.ui.base.android.R as BaseR

/**
 * Pelnoekranowy podglad zdjec. Galeria wpisu (do 4 zdjec) to kolejne strony
 * pagera - przesuwanie w bok, pozycja "2 / 4" w tytule; pojedyncze zdjecie to
 * pager z jedna strona. Akcje z menu dzialaja na biezacej stronie.
 */
internal class PhotoViewActivity : BaseActivity() {
    // Pelnoekranowy podglad zdjecia - bez paddingu od belki nawigacji.
    override val applyWindowInsetsToContent = false

    companion object {
        const val URLS_EXTRA = "URLS"
        const val INDEX_EXTRA = "INDEX"
        const val LABELS_EXTRA = "LABELS"
        const val SHARE_REQUEST_CODE = 1

        fun createIntent(
            context: Context,
            imageUrl: String,
        ) = createIntent(context, listOf(imageUrl), 0)

        /** [labels] rownolegle do [imageUrls]; brak etykiety = null. */
        fun createIntent(
            context: Context,
            imageUrls: List<String>,
            index: Int,
            labels: List<String?> = emptyList(),
        ) = Intent(context, PhotoViewActivity::class.java).apply {
            putStringArrayListExtra(URLS_EXTRA, ArrayList(imageUrls))
            putExtra(INDEX_EXTRA, index)
            // Intent nie przenosi nulli w liscie stringow - brak etykiety jako "".
            putStringArrayListExtra(LABELS_EXTRA, ArrayList(imageUrls.indices.map { labels.getOrNull(it).orEmpty() }))
        }
    }

    @Inject
    lateinit var clipboardHelper: ClipboardHelperApi

    private val binding by viewBinding(ActivityPhotoviewBinding::inflate)

    override val enableSwipeBackLayout: Boolean = true // We manually attach it here
    override val isActivityTransfluent: Boolean = true

    private lateinit var urls: List<String>
    private var labels: List<String> = emptyList()
    private val photoViewActions: PhotoViewCallbacks by lazy { PhotoViewActions(this) }

    /** Adres zdjecia na biezacej stronie. */
    val url: String
        get() = urls[binding.pager.currentItem]

    /** Etykieta zdjecia na biezacej stronie (null gdy API jej nie podalo). */
    private val label: String?
        get() = labels.getOrNull(binding.pager.currentItem)?.takeIf { it.isNotBlank() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar.toolbar)
        binding.toolbar.toolbar.setBackgroundResource(BaseR.drawable.gradient_toolbar_up)
        urls = intent.getStringArrayListExtra(URLS_EXTRA)?.takeIf { it.isNotEmpty() } ?: return finish()
        labels = intent.getStringArrayListExtra(LABELS_EXTRA).orEmpty()
        binding.pager.adapter = PhotoPagesAdapter(this, urls)
        binding.pager.setCurrentItem(intent.getIntExtra(INDEX_EXTRA, 0).coerceIn(urls.indices), false)
        updateTitle()
        binding.pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = updateTitle()
            },
        )

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroy() {
        // Odpiecie adaptera recyklinguje strony (onViewRecycled) - bez tego
        // nasluchy postepu w GlideProgressSupport zostalyby zarejestrowane.
        binding.pager.adapter = null
        super.onDestroy()
    }

    // "2 / 4 · etykieta"; pojedyncze zdjecie - sama etykieta (albo pusty tytul).
    private fun updateTitle() {
        val position = if (urls.size > 1) getString(R.string.photo_view_position, binding.pager.currentItem + 1, urls.size) else null
        title = listOfNotNull(position, label).joinToString(" · ").ifEmpty { null }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.photoview_menu, menu)
        menu.findItem(R.id.action_save_mp4)?.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_share -> photoViewActions.shareImage(url)
            R.id.action_save_image -> photoViewActions.saveImage(url, label)
            R.id.action_copy_url -> clipboardHelper.copyTextToClipboard(url, "imageUrl")
            R.id.action_open_browser -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            android.R.id.home -> finish()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}

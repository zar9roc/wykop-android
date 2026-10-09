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
        const val SHARE_REQUEST_CODE = 1

        fun createIntent(
            context: Context,
            imageUrl: String,
        ) = createIntent(context, listOf(imageUrl), 0)

        fun createIntent(
            context: Context,
            imageUrls: List<String>,
            index: Int,
        ) = Intent(context, PhotoViewActivity::class.java).apply {
            putStringArrayListExtra(URLS_EXTRA, ArrayList(imageUrls))
            putExtra(INDEX_EXTRA, index)
        }
    }

    @Inject
    lateinit var clipboardHelper: ClipboardHelperApi

    private val binding by viewBinding(ActivityPhotoviewBinding::inflate)

    override val enableSwipeBackLayout: Boolean = true // We manually attach it here
    override val isActivityTransfluent: Boolean = true

    private lateinit var urls: List<String>
    private val photoViewActions: PhotoViewCallbacks by lazy { PhotoViewActions(this) }

    /** Adres zdjecia na biezacej stronie. */
    val url: String
        get() = urls[binding.pager.currentItem]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar.toolbar)
        binding.toolbar.toolbar.setBackgroundResource(BaseR.drawable.gradient_toolbar_up)
        urls = intent.getStringArrayListExtra(URLS_EXTRA)?.takeIf { it.isNotEmpty() } ?: return finish()
        title = null
        binding.pager.adapter = PhotoPagesAdapter(this, urls)
        binding.pager.setCurrentItem(intent.getIntExtra(INDEX_EXTRA, 0).coerceIn(urls.indices), false)
        if (urls.size > 1) {
            updateTitle(binding.pager.currentItem)
            binding.pager.registerOnPageChangeCallback(
                object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) = updateTitle(position)
                },
            )
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onDestroy() {
        // Odpiecie adaptera recyklinguje strony (onViewRecycled) - bez tego
        // nasluchy postepu w GlideProgressSupport zostalyby zarejestrowane.
        binding.pager.adapter = null
        super.onDestroy()
    }

    private fun updateTitle(position: Int) {
        title = getString(R.string.photo_view_position, position + 1, urls.size)
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
}

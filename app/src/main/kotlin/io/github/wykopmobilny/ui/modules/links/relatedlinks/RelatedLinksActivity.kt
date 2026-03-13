package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.databinding.ActivityRelatedLinksBinding
import io.github.wykopmobilny.utils.bindings.bindBackButton
import io.github.wykopmobilny.utils.viewBinding

internal class RelatedLinksActivity : ThemableActivity() {
    private val binding by viewBinding(ActivityRelatedLinksBinding::inflate)

    private val linkId
        get() =
            intent
                .takeIf { it.hasExtra(EXTRA_LINK_ID) }
                ?.getLongExtra(EXTRA_LINK_ID, 0L)
                .let(::checkNotNull)

    private var relatedLinksFragment: RelatedLinksFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.bindBackButton(activity = this)
        supportActionBar?.title = "Powiązane"

        if (savedInstanceState == null) {
            relatedLinksFragment = RelatedLinksFragment.newInstance(linkId = linkId)
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, relatedLinksFragment!!)
                .commit()
        } else {
            relatedLinksFragment =
                supportFragmentManager.findFragmentById(binding.fragmentContainer.id) as? RelatedLinksFragment
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.related_links_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.refresh -> {
                relatedLinksFragment?.refresh()
                true
            }

            R.id.add_related -> {
                relatedLinksFragment?.showAddRelatedDialog()
                true
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    companion object {
        const val EXTRA_LINK_ID = "EXTRA_LINKID"

        fun createIntent(
            context: Context,
            linkId: Long,
        ) = Intent(context, RelatedLinksActivity::class.java).apply {
            putExtra(EXTRA_LINK_ID, linkId)
        }
    }
}

package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.databinding.ActivityContainerBinding
import io.github.wykopmobilny.utils.bindings.bindBackButton
import io.github.wykopmobilny.utils.viewBinding

internal class RelatedLinksActivity : ThemableActivity() {
    private val binding by viewBinding(ActivityContainerBinding::inflate)

    private val linkId
        get() =
            intent
                .takeIf { it.hasExtra(EXTRA_LINK_ID) }
                ?.getLongExtra(EXTRA_LINK_ID, 0L)
                .let(::checkNotNull)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val toolbar = binding.root.findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            toolbar.bindBackButton(activity = this)
            supportActionBar?.title = "Powiązane linki"
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, RelatedLinksFragment.newInstance(linkId = linkId))
                .commit()
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

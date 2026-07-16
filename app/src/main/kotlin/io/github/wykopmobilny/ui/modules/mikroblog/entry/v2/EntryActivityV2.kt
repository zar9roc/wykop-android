package io.github.wykopmobilny.ui.modules.mikroblog.entry.v2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.databinding.ActivityContainerBinding
import io.github.wykopmobilny.ui.modules.input.BaseInputActivity
import io.github.wykopmobilny.utils.viewBinding

/**
 * Ekran wpisu na nowym stacku (domain + coroutines + dwukierunkowa paginacja).
 * Powłoka jak LinkDetailsActivityV2 - nie-daggerowa ThemableActivity, fragment
 * injectuje się sam przez AndroidSupportInjection.
 */
internal class EntryActivityV2 : ThemableActivity() {
    private val binding by viewBinding(ActivityContainerBinding::inflate)

    private val entryId: Long
        get() = intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it > 0 }.let(::checkNotNull)

    private val commentId: Long?
        get() = intent.getLongExtra(EXTRA_COMMENT_ID, -1L).takeIf { it > 0 }

    private val page: Int?
        get() = intent.getIntExtra(EXTRA_PAGE, -1).takeIf { it > 0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    binding.fragmentContainer.id,
                    EntryDetailsFragment.newInstance(entryId = entryId, commentId = commentId, page = page),
                ).commit()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        // Edycja wpisu/komentarza wraca przez startActivityForResult (NewNavigator
        // startuje z kontekstu aktywności) - odświeżenie delegujemy do fragmentu.
        if (resultCode == RESULT_OK &&
            (requestCode == BaseInputActivity.EDIT_ENTRY || requestCode == BaseInputActivity.EDIT_ENTRY_COMMENT)
        ) {
            supportFragmentManager.fragments
                .filterIsInstance<EntryDetailsFragment>()
                .forEach(EntryDetailsFragment::onContentEdited)
        }
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
        private const val EXTRA_PAGE = "EXTRA_PAGE"

        fun createIntent(
            context: Context,
            entryId: Long,
            commentId: Long? = null,
            page: Int? = null,
        ) = Intent(context, EntryActivityV2::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, entryId)
            commentId?.let { putExtra(EXTRA_COMMENT_ID, it) }
            page?.let { putExtra(EXTRA_PAGE, it) }
        }
    }
}

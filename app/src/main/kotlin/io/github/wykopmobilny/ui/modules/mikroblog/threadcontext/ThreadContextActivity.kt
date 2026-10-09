package io.github.wykopmobilny.ui.modules.mikroblog.threadcontext

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.ui.modules.input.BaseInputActivity
import io.github.wykopmobilny.databinding.ActivityContainerBinding
import io.github.wykopmobilny.utils.viewBinding

/**
 * Powloka ekranu kontekstu watku - jak EntryActivityV2: nie-daggerowa
 * ThemableActivity, fragment injectuje sie sam przez AndroidSupportInjection.
 */
internal class ThreadContextActivity : ThemableActivity() {
    private val binding by viewBinding(ActivityContainerBinding::inflate)

    private val entryId: Long
        get() = intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it > 0 }.let(::checkNotNull)

    private val commentId: Long
        get() = intent.getLongExtra(EXTRA_COMMENT_ID, -1L).takeIf { it > 0 }.let(::checkNotNull)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    binding.fragmentContainer.id,
                    ThreadContextFragment.newInstance(entryId = entryId, commentId = commentId),
                ).commit()
        }
    }

    // Edycja wpisu/komentarza z tego ekranu wraca tutaj (NewNavigator startuje z
    // kontekstu aktywnosci) - bez przeladowania zostawala stara wersja.
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK &&
            (requestCode == BaseInputActivity.EDIT_ENTRY || requestCode == BaseInputActivity.EDIT_ENTRY_COMMENT)
        ) {
            supportFragmentManager.fragments
                .filterIsInstance<ThreadContextFragment>()
                .forEach(ThreadContextFragment::onContentEdited)
        }
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"
        private const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"

        fun createIntent(
            context: Context,
            entryId: Long,
            commentId: Long,
        ) = Intent(context, ThreadContextActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, entryId)
            putExtra(EXTRA_COMMENT_ID, commentId)
        }
    }
}

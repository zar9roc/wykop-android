package io.github.wykopmobilny.ui.modules.links.linkdetails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.wykopmobilny.base.ThemableActivity
import io.github.wykopmobilny.databinding.ActivityContainerBinding
import io.github.wykopmobilny.ui.modules.input.BaseInputActivity
import io.github.wykopmobilny.utils.viewBinding

/**
 * "Odpowiedz"/"Cytuj" pod komentarzem znaleziska na liscie (np. profil): pasek odpowiedzi jest
 * na ekranie znaleziska, wiec przekazujemy tam watek ([threadId] = id komentarza glownego,
 * [threadAuthor] = jego autor do paska "w watku @nick") i tresc do wstawienia w pole.
 */
internal data class PendingLinkReply(
    val threadId: Long,
    val threadAuthor: String?,
    val replyToAuthor: String? = null,
    val quoteAuthor: String? = null,
    val quoteBody: String? = null,
)

internal class LinkDetailsActivityV2 : ThemableActivity() {
    private val binding by viewBinding(ActivityContainerBinding::inflate)

    private val linkId
        get() =
            intent
                .takeIf { it.hasExtra(EXTRA_LINK_ID) }
                ?.getLongExtra(EXTRA_LINK_ID, 0L)
                .let(::checkNotNull)
    private val commentId
        get() =
            intent
                .takeIf { it.hasExtra(EXTRA_COMMENT_ID) }
                ?.getLongExtra(EXTRA_COMMENT_ID, 0L)

    private val pendingReply
        get() =
            intent
                .takeIf { it.hasExtra(EXTRA_REPLY_THREAD_ID) }
                ?.let {
                    PendingLinkReply(
                        threadId = it.getLongExtra(EXTRA_REPLY_THREAD_ID, 0L),
                        threadAuthor = it.getStringExtra(EXTRA_REPLY_THREAD_AUTHOR),
                        replyToAuthor = it.getStringExtra(EXTRA_REPLY_TO_AUTHOR),
                        quoteAuthor = it.getStringExtra(EXTRA_QUOTE_AUTHOR),
                        quoteBody = it.getStringExtra(EXTRA_QUOTE_BODY),
                    )
                }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(
                    binding.fragmentContainer.id,
                    LinkDetailsFragment.newInstance(linkId = linkId, commentId = commentId, reply = pendingReply),
                ).commit()
        }
    }

    // Edycja komentarza wraca przez startActivityForResult (NewNavigator startuje z biezacej
    // aktywnosci) - przeladowanie komentarzy delegujemy do fragmentu, jak w EntryActivityV2.
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == BaseInputActivity.EDIT_LINK_COMMENT) {
            supportFragmentManager.fragments
                .filterIsInstance<LinkDetailsFragment>()
                .forEach(LinkDetailsFragment::onCommentEdited)
        }
    }

    companion object {
        const val EXTRA_LINK_ID = "EXTRA_LINKID"
        const val EXTRA_COMMENT_ID = "EXTRA_COMMENT_ID"
        private const val EXTRA_REPLY_THREAD_ID = "EXTRA_REPLY_THREAD_ID"
        private const val EXTRA_REPLY_THREAD_AUTHOR = "EXTRA_REPLY_THREAD_AUTHOR"
        private const val EXTRA_REPLY_TO_AUTHOR = "EXTRA_REPLY_TO_AUTHOR"
        private const val EXTRA_QUOTE_AUTHOR = "EXTRA_QUOTE_AUTHOR"
        private const val EXTRA_QUOTE_BODY = "EXTRA_QUOTE_BODY"

        fun createIntent(
            context: Context,
            linkId: Long,
            commentId: Long? = null,
            reply: PendingLinkReply? = null,
        ) = Intent(context, LinkDetailsActivityV2::class.java).apply {
            putExtra(EXTRA_LINK_ID, linkId)
            commentId?.let { putExtra(EXTRA_COMMENT_ID, it) }
            if (reply != null) {
                putExtra(EXTRA_REPLY_THREAD_ID, reply.threadId)
                putExtra(EXTRA_REPLY_THREAD_AUTHOR, reply.threadAuthor)
                putExtra(EXTRA_REPLY_TO_AUTHOR, reply.replyToAuthor)
                putExtra(EXTRA_QUOTE_AUTHOR, reply.quoteAuthor)
                putExtra(EXTRA_QUOTE_BODY, reply.quoteBody)
            }
        }
    }
}

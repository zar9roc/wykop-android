package io.github.wykopmobilny.ui.adapters.viewholders

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkCommentMenuBottomsheetBinding
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.ui.dialogs.confirmationDialog
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentActionListener
import io.github.wykopmobilny.ui.fragments.linkcomments.LinkCommentViewListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.ui.widgets.WykopEmbedView
import io.github.wykopmobilny.ui.widgets.buttons.MinusVoteButton
import io.github.wykopmobilny.ui.widgets.buttons.PlusVoteButton
import io.github.wykopmobilny.utils.copyText
import io.github.wykopmobilny.utils.getActivityContext
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.textview.prepareBody
import io.github.wykopmobilny.utils.textview.stripWykopFormatting
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import kotlin.math.absoluteValue

abstract class BaseLinkCommentViewHolder(
    view: View,
    private val userManagerApi: UserManagerApi,
    protected val navigator: NewNavigator,
    private val linkHandler: WykopLinkHandler,
    protected val commentViewListener: LinkCommentViewListener?,
    private val commentActionListener: LinkCommentActionListener,
) : RecyclableViewHolder(view) {
    companion object {
        fun getViewTypeForComment(
            comment: LinkCommentV3Item,
            forceTop: Boolean = false,
        ): Int =
            if (comment.parentId != comment.id && !forceTop) {
                when {
                    comment.deletedReason != null -> LinkCommentViewHolder.TYPE_NORMAL
                    comment.isBlocked -> LinkCommentViewHolder.TYPE_BLOCKED
                    comment.embed == null -> LinkCommentViewHolder.TYPE_NORMAL
                    else -> LinkCommentViewHolder.TYPE_EMBED
                }
            } else {
                when {
                    comment.deletedReason != null -> TopLinkCommentViewHolder.TYPE_TOP_NORMAL
                    comment.isBlocked -> TopLinkCommentViewHolder.TYPE_TOP_BLOCKED
                    comment.embed == null -> TopLinkCommentViewHolder.TYPE_TOP_NORMAL
                    else -> TopLinkCommentViewHolder.TYPE_TOP_EMBED
                }
            }
    }

    private val userAuthorized by lazy { userManagerApi.isUserAuthorized() }
    private val userCredentials by lazy { userManagerApi.getUserCredentials() }

    private val collapseDrawable: Drawable? by lazy {
        itemView.context
            .obtainStyledAttributes(arrayOf(R.attr.collapseDrawable).toIntArray())
            .use { it.getDrawable(0) }
    }

    private val expandDrawable: Drawable? by lazy {
        itemView.context
            .obtainStyledAttributes(arrayOf(R.attr.expandDrawable).toIntArray())
            .use { it.getDrawable(0) }
    }

    var type: Int = 0
    abstract var embedView: WykopEmbedView
    abstract var commentContent: TextView
    abstract var replyButton: TextView
    abstract var quoteButton: TextView
    abstract var collapseButton: ImageView
    abstract var authorBadgeStrip: View
    abstract var plusButton: PlusVoteButton
    abstract var minusButton: MinusVoteButton
    abstract var moreOptionsButton: TextView
    abstract var shareButton: TextView
    open var collapsedCommentsTextView: TextView? = null

    open fun bindView(
        linkComment: LinkCommentV3Item,
        isAuthorComment: Boolean,
        commentId: Long = -1,
        openSpoilersDialog: Boolean,
        enableYoutubePlayer: Boolean,
        enableEmbedPlayer: Boolean,
        showAdultContent: Boolean,
        hideNsfw: Boolean,
    ) {
        setupBody(
            comment = linkComment,
            openSpoilersDialog = openSpoilersDialog,
            enableYoutubePlayer = enableYoutubePlayer,
            enableEmbedPlayer = enableEmbedPlayer,
            showAdultContent = showAdultContent,
            hideNsfw = hideNsfw,
        )
        setupButtons(linkComment)
        setStyleForComment(linkComment, isAuthorComment, commentId)
    }

    private fun setupBody(
        comment: LinkCommentV3Item,
        openSpoilersDialog: Boolean,
        enableYoutubePlayer: Boolean,
        enableEmbedPlayer: Boolean,
        showAdultContent: Boolean,
        hideNsfw: Boolean,
    ) {
        val isDeleted = comment.deletedReason != null

        // Reply button - visible if authorized, disabled if deleted
        replyButton.isVisible = userAuthorized && commentViewListener != null
        replyButton.isEnabled = !isDeleted

        // Quote button - visible if authorized, disabled if deleted
        quoteButton.isVisible = userAuthorized && commentViewListener != null
        quoteButton.isEnabled = !isDeleted
        quoteButton.setOnClickListener { commentViewListener?.quoteComment(comment) }

        if (isDeleted) {
            setupDeletedBody(comment)
        } else {
            if (type == LinkCommentViewHolder.TYPE_EMBED || type == TopLinkCommentViewHolder.TYPE_TOP_EMBED) {
                embedView.setEmbed(
                    embed = comment.embed,
                    enableYoutubePlayer = enableYoutubePlayer,
                    enableEmbedPlayer = enableEmbedPlayer,
                    showAdultContent = showAdultContent,
                    hideNsfw = hideNsfw,
                    navigator = navigator,
                    isNsfw = comment.isNsfw,
                )
            }

            val body = comment.body
            if (!body.isNullOrEmpty()) {
                commentContent.isVisible = true
                resetContentTextViewStyle()
                commentContent.prepareBody(
                    html = body,
                    urlClickListener = linkHandler::handleUrl,
                    clickListener = { handleClick(comment) },
                    openSpoilersDialog = openSpoilersDialog,
                )
            } else {
                commentContent.isVisible = false
            }
        }

        itemView.setOnClickListener { handleClick(comment) }

        collapseButton.isVisible =
            !((comment.id != comment.parentId) || comment.childCommentCount == 0) && commentViewListener != null
    }

    private fun resetContentTextViewStyle() {
        val textColor = TypedValue()
        itemView.context.theme.resolveAttribute(android.R.attr.textColorPrimary, textColor, true)
        commentContent.setTextColor(textColor.data)
        commentContent.setTypeface(null, android.graphics.Typeface.NORMAL)
        commentContent.setOnClickListener(null)
    }

    private fun setupDeletedBody(comment: LinkCommentV3Item) {
        val context = itemView.context
        val deletedText =
            when (comment.deletedReason) {
                "host" -> context.getString(R.string.comment_deleted_by_host)
                "moderator" -> context.getString(R.string.comment_deleted_by_moderator)
                "author" -> context.getString(R.string.comment_deleted_by_author)
                else -> context.getString(R.string.comment_deleted_generic)
            }

        val greyColor = TypedValue()
        context.theme.resolveAttribute(R.attr.textColorGrey, greyColor, true)

        commentContent.isVisible = true
        commentContent.setTextColor(greyColor.data)
        commentContent.text = deletedText
        commentContent.setTypeface(
            commentContent.typeface,
            android.graphics.Typeface.ITALIC,
        )

        if (!comment.slug.isNullOrEmpty()) {
            commentContent.setOnClickListener {
                showSlugDialog(comment.slug)
            }
        } else {
            commentContent.setOnClickListener(null)
        }
    }

    private fun showSlugDialog(slug: String?) {
        if (slug.isNullOrEmpty()) return
        val context = itemView.getActivityContext() ?: return
        AlertDialog
            .Builder(context)
            .setTitle(R.string.deleted_comment_content)
            .setMessage(slug)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun handleClick(comment: LinkCommentV3Item) {
        // Register click listener for comments list
        if (commentViewListener == null) {
            navigator.openLinkDetailsActivity(comment.linkId, comment.id)
        }
    }

    private fun setStyleForComment(
        comment: LinkCommentV3Item,
        isAuthorComment: Boolean,
        commentId: Long = -1,
    ) {
        if (userCredentials?.login == comment.author.nick) {
            authorBadgeStrip.isVisible = true
            authorBadgeStrip.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorBadgeOwn))
        } else if (isAuthorComment) {
            authorBadgeStrip.isVisible = true
            authorBadgeStrip.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.colorBadgeAuthors))
        } else {
            authorBadgeStrip.isVisible = false
        }

        if (commentId == comment.id) {
            authorBadgeStrip.isVisible = true
            authorBadgeStrip.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.plusPressedColor))
        }
    }

    private fun setupButtons(comment: LinkCommentV3Item) {
        val isDeleted = comment.deletedReason != null

        plusButton.setup(userManagerApi)
        plusButton.text = comment.voteCountPlus.toString()
        minusButton.setup(userManagerApi)
        minusButton.text = comment.voteCountMinus.absoluteValue.toString()

        // More options - always visible (menu has useful options even for deleted comments)
        moreOptionsButton.isVisible = true
        moreOptionsButton.setOnClickListener { openLinkCommentMenu(comment) }

        // Share button - always visible but disabled if deleted
        shareButton.isVisible = true
        shareButton.isEnabled = !isDeleted
        shareButton.setOnClickListener {
            navigator.shareUrl(comment.url)
        }

        // Vote buttons - always visible but disabled if deleted
        plusButton.isVisible = true
        plusButton.isEnabled = !isDeleted
        minusButton.isVisible = true
        minusButton.isEnabled = !isDeleted

        if (comment.isCollapsed) {
            collapseButton.setImageDrawable(expandDrawable)
            collapseButton.setOnClickListener {
                commentViewListener?.setCollapsed(comment, false)
                collapsedCommentsTextView?.isVisible = comment.childCommentCount > 0
                collapsedCommentsTextView?.text = "${comment.childCommentCount} ukrytych komentarzy"
            }
        } else {
            collapseButton.setImageDrawable(collapseDrawable)
            collapseButton.setOnClickListener {
                collapsedCommentsTextView?.isVisible = false
                commentViewListener?.setCollapsed(comment, true)
            }
        }

        plusButton.voteListener = {
            commentActionListener.digComment(comment)
            minusButton.isEnabled = false
        }

        minusButton.voteListener = {
            commentActionListener.buryComment(comment)
            plusButton.isEnabled = false
        }

        plusButton.unvoteListener = {
            minusButton.isEnabled = false
            commentActionListener.removeVote(comment)
        }

        minusButton.unvoteListener = {
            plusButton.isEnabled = false
            commentActionListener.removeVote(comment)
        }

        when (comment.userVote) {
            1 -> {
                plusButton.isButtonSelected = true
                minusButton.isButtonSelected = false
            }

            0 -> {
                plusButton.isButtonSelected = false
                minusButton.isButtonSelected = false
            }

            -1 -> {
                plusButton.isButtonSelected = false
                minusButton.isButtonSelected = true
            }
        }

        replyButton.setOnClickListener {
            commentViewListener?.replyComment(comment)
        }
    }

    private fun openLinkCommentMenu(comment: LinkCommentV3Item) {
        val activityContext = itemView.getActivityContext() ?: return
        val dialog = BottomSheetDialog(activityContext)
        val bottomSheetView = LinkCommentMenuBottomsheetBinding.inflate(activityContext.layoutInflater)
        dialog.setContentView(bottomSheetView.root)
        (bottomSheetView.root.parent as View).setBackgroundColor(Color.TRANSPARENT)
        val isDeleted = comment.deletedReason != null

        bottomSheetView.apply {
            author.text = comment.author.nick
            val dateAsString =
                comment.fullDate
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .run {
                        LocalDateTime.of(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond)
                    }.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
            date.text = comment.app?.takeIf { it.isNotEmpty() }?.let {
                root.context.getString(R.string.date_with_user_app, dateAsString, comment.app)
            } ?: dateAsString

            // Copy - copies slug if deleted and available, otherwise body
            commentMenuCopy.setOnClickListener {
                val textToCopy =
                    if (isDeleted && !comment.slug.isNullOrEmpty()) {
                        comment.slug.orEmpty()
                    } else {
                        comment.body?.stripWykopFormatting() ?: ""
                    }
                it.context.copyText(textToCopy, "link-comment-body")

                dialog.dismiss()
            }

            commentMenuDelete.setOnClickListener {
                val context = it.getActivityContext() ?: return@setOnClickListener
                confirmationDialog(context) { commentActionListener.deleteComment(comment) }
                    .show()
                dialog.dismiss()
            }

            commentMenuReport.isVisible = !isDeleted && userManagerApi.isUserAuthorized() && comment.violationUrl != null
            commentMenuReport.setOnClickListener {
                navigator.openReportScreen(comment.violationUrl.let(::checkNotNull))
                dialog.dismiss()
            }

            commentMenuEdit.setOnClickListener {
                navigator.openEditLinkCommentActivity(comment.linkId, comment.body.orEmpty(), comment.id)
                dialog.dismiss()
            }

            val canUserEdit = comment.author.nick == userManagerApi.getUserCredentials()?.login
            commentMenuDelete.isVisible = !isDeleted && canUserEdit
            commentMenuEdit.isVisible = !isDeleted && canUserEdit
        }

        val mBehavior = BottomSheetBehavior.from(bottomSheetView.root.parent as View)
        dialog.setOnShowListener { mBehavior.peekHeight = bottomSheetView.root.height }
        dialog.show()
    }
}

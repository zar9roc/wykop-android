package io.github.wykopmobilny.ui.modules.links.linkdetails.items

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.core.view.isVisible
import com.github.wykopmobilny.ui.components.toColorInt
import com.github.wykopmobilny.ui.components.utils.readColorAttr
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.LinkCommentLayoutBinding
import io.github.wykopmobilny.databinding.TopLinkCommentLayoutBinding
import io.github.wykopmobilny.links.details.LinkCommentUi
import io.github.wykopmobilny.links.details.ParentCommentUi
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.ui.components.widgets.ColorConst
import io.github.wykopmobilny.ui.components.widgets.EmbedMediaUi
import io.github.wykopmobilny.ui.widgets.WykopEmbedView
import io.github.wykopmobilny.utils.bindings.setOnClick
import io.github.wykopmobilny.utils.bindings.setOnLongClick
import io.github.wykopmobilny.utils.textview.BetterLinkMovementMethod
import androidx.appcompat.R as AppcompatR
import io.github.wykopmobilny.ui.base.android.R as BaseR

// Delikatne zolte tlo komentarza docelowego (nawigacja z powiadomienia) - ~15% alpha,
// czytelne na jasnym i ciemnym motywie.
private val linkedCommentBackground = 0x26FFC107.toInt()

// Layouty komentarzy maja ViewStub z domyslnym WykopEmbedView (@layout/stub_embed).
// Renderujemy embed 1:1 jak na mikroblogu (miniatura + ikona dostawcy w rogu, badge
// GIF, zaslona 18+/nsfw). Klikniecie deleguje do akcji z domeny (clickAction), wiec
// nie potrzeba tu nawigatora ani ustawien odtwarzacza. Po inflate stub znika z
// hierarchii, wiec kolejne bindy znajduja widok po id.
private fun bindEmbedV3(
    root: View,
    stub: ViewStub,
    embed: EmbedMediaUi?,
) {
    if (embed == null) {
        hideEmbedV3(root)
        return
    }
    val embedView =
        (root.findViewById<View>(R.id.wykopEmbedView) as? WykopEmbedView)
            ?: (stub.inflate() as WykopEmbedView)
    val realPreview =
        when (val content = embed.content) {
            is EmbedMediaUi.Content.StaticImage -> content.url
            is EmbedMediaUi.Content.PlayableMedia -> content.previewImage
        }
    // Zaslona 18+/nsfw jest w calosci sterowana przez domene: gdy overlay != null
    // pokazujemy placeholder, a klikniecie (clickAction) odslania obraz przez viewState
    // (flow przeladowuje komentarz juz z overlay=null). WLASNA zaslona WykopEmbedView
    // jest tu WYLACZONA (showAdultContent=true, hideNsfw=false, isNsfw=false) - inaczej
    // byly dwie niezalezne zaslony i odslanianie zabieralo dodatkowe (trzecie) tapniecie.
    val displayPreview =
        when (embed.overlay) {
            EmbedMediaUi.Overlay.Nsfw -> WykopEmbedView.NSFW_IMAGE_PLACEHOLDER
            EmbedMediaUi.Overlay.Plus18 -> WykopEmbedView.PLUS18_IMAGE_PLACEHOLDER
            null -> realPreview
        }
    val appEmbed =
        Embed(
            type = if (embed.content is EmbedMediaUi.Content.PlayableMedia) "video" else "image",
            preview = displayPreview,
            url = embed.url ?: realPreview,
            plus18 = false,
            source = null,
            isAnimated = embed.isAnimated,
            size = embed.size.orEmpty(),
        )
    embedView.isVisible = true
    embedView.onEmbedClickOverride = embed.clickAction
    embedView.setEmbed(
        embed = appEmbed,
        enableYoutubePlayer = false,
        enableEmbedPlayer = false,
        showAdultContent = true,
        hideNsfw = false,
        navigator = null,
        isNsfw = false,
    )
}

private fun hideEmbedV3(root: View) {
    (root.findViewById<View>(R.id.wykopEmbedView) as? WykopEmbedView)?.isVisible = false
}

// Zwiniety watek: strzalka w dol (mozna rozwinac), rozwiniety: w gore (mozna zwinac).
private fun Context.chevronFor(collapsed: Boolean): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(if (collapsed) R.attr.expandDrawable else R.attr.collapseDrawable, typedValue, true)
    return typedValue.resourceId
}

// --- Parent comment (top_link_comment_layout.xml) ---

@Suppress("UnusedParameter")
internal fun TopLinkCommentLayoutBinding.bindParentCommentV3(
    parent: ParentCommentUi,
    data: LinkCommentUi.Normal,
    hasReplies: Boolean,
    // Wstawianie tekstu do pola odpowiedzi to operacja czysto widokowa
    // (InputToolbar w LinkDetailsFragment) - stad callbacki z adaptera,
    // nie akcje domeny. null = uzytkownik niezalogowany, przyciski ukryte.
    onReply: (() -> Unit)?,
    onQuote: (() -> Unit)?,
) {
    root.setOnClick(data.clickAction)
    root.setOnLongClick(parent.toggleExpansionStateAction)
    if (data.showsOption) {
        root.setBackgroundColor(
            root.context.readColorAttr(AppcompatR.attr.colorControlHighlight).defaultColor,
        )
    } else if (data.badge == ColorConst.CommentLinked) {
        root.setBackgroundColor(linkedCommentBackground)
    } else {
        root.background = null
    }

    // Collapse button
    collapseButtonImageView.isVisible = parent.toggleExpansionStateAction != null
    collapseButtonImageView.setOnClick(parent.toggleExpansionStateAction)
    collapseButtonImageView.setImageResource(root.context.chevronFor(collapsed = parent.collapsedCount != null))

    // Author — bind AuthorHeaderView's internal views
    val authorAvatarView = authorHeaderView.findViewById<android.view.View>(R.id.authorAvatarView)
    authorAvatarView?.bindAvatarV3(data.author.avatar)
    authorAvatarView?.setOnClick(data.profileAction)
    val nameView = authorHeaderView.findViewById<TextView>(R.id.userNameTextView)
    if (nameView != null) {
        nameView.text = data.author.name
        val authorColor =
            data.author.color?.toColorInt(nameView.context)
                ?: nameView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
        nameView.setTextColor(authorColor)
        nameView.setOnClick(data.profileAction)
    }
    val dateView = authorHeaderView.findViewById<TextView>(R.id.entryDateTextView)
    if (dateView != null) {
        dateView.text = data.app?.let { "${data.postedAgo} via $it" } ?: data.postedAgo
    }

    // Body
    commentContentTextView.text = data.body.content
    commentContentTextView.isVisible = data.body.content != null
    // Spany z domeny (wzmianki/tagi/URL-e) potrzebuja movement method, zeby dostawac
    // klikniecia; dotyk poza linkiem propaguje sie do klikniecia wiersza.
    BetterLinkMovementMethod.linkifyHtml(commentContentTextView)

    // Embed
    bindEmbedV3(root, wykopEmbedView, data.embed)

    // Badge
    authorBadgeStripView.setBackgroundColor(
        data.badge.toColorInt(context = root.context).defaultColor,
    )

    // Vote buttons — bind directly without VoteButton.setup().
    // isButtonSelected przelacza ikone na wersje kolorowa (ic_*_activ) jak na
    // mikroblogu; color != null z domeny = uzytkownik oddal ten glos.
    plusVoteButton.text = data.plusCount.label
    plusVoteButton.isButtonSelected = data.plusCount.color != null
    val plusColor =
        data.plusCount.color?.toColorInt(plusVoteButton.context)
            ?: plusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    plusVoteButton.setTextColor(plusColor)
    plusVoteButton.setOnClick(data.plusCount.clickAction)

    minusVoteButton.text = data.minusCount.label
    minusVoteButton.isButtonSelected = data.minusCount.color != null
    val minusColor =
        data.minusCount.color?.toColorInt(minusVoteButton.context)
            ?: minusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    minusVoteButton.setTextColor(minusColor)
    minusVoteButton.setOnClick(data.minusCount.clickAction)

    // Action buttons
    shareTextView.setOnClick(data.shareAction)
    moreOptionsTextView.setOnClick(data.moreAction)
    replyTextView.isVisible = onReply != null
    replyTextView.setOnClick(onReply)
    quoteTextView.isVisible = onQuote != null
    quoteTextView.setOnClick(onQuote)

    // Zwiniety watek: pokazujemy "N ukrytych komentarzy" (jak mikroblog / stary widok
    // linku) zamiast samej strzalki - klikniecie etykiety rozwija watek.
    val collapsedCount = parent.collapsedCount
    if (collapsedCount != null) {
        messageTextView.isVisible = true
        messageTextView.text =
            root.context.getString(R.string.collapsed_replies, collapsedCount.removePrefix("+"))
        messageTextView.setOnClick(parent.toggleExpansionStateAction)
    } else {
        messageTextView.isVisible = false
    }
}

// --- Reply comment (link_comment_layout.xml) ---

@Suppress("UnusedParameter")
internal fun LinkCommentLayoutBinding.bindReplyCommentV3(
    comment: LinkCommentUi.Normal,
    isLast: Boolean,
    onReply: (() -> Unit)?,
    onQuote: (() -> Unit)?,
) {
    root.setOnClick(comment.clickAction)
    if (comment.showsOption) {
        root.setBackgroundColor(
            root.context.readColorAttr(AppcompatR.attr.colorControlHighlight).defaultColor,
        )
    } else if (comment.badge == ColorConst.CommentLinked) {
        root.setBackgroundColor(linkedCommentBackground)
    } else {
        root.background = null
    }

    // Author
    avatarView.bindAvatarV3(comment.author.avatar)
    avatarView.setOnClick(comment.profileAction)
    authorTextView.text = comment.author.name
    val authorColor =
        comment.author.color?.toColorInt(authorTextView.context)
            ?: authorTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    authorTextView.setTextColor(authorColor)

    // Date
    dateTextView.text = comment.app?.let { "${comment.postedAgo} via $it" } ?: comment.postedAgo

    // Collapse button — visible for reply comments
    collapseButtonImageView.isVisible = false

    // Body
    commentContentTextView.text = comment.body.content
    commentContentTextView.isVisible = comment.body.content != null
    BetterLinkMovementMethod.linkifyHtml(commentContentTextView)

    // Embed
    bindEmbedV3(root, wykopEmbedView, comment.embed)

    // Badge
    val badgeColor =
        (
            comment.badge?.toColorInt(context = root.context)
                ?: root.context.readColorAttr(BaseR.attr.colorDivider)
        ).defaultColor
    authorBadgeStripView.setBackgroundColor(badgeColor)

    // Vote buttons — isButtonSelected koloruje ikone (jak na mikroblogu),
    // color != null z domeny = uzytkownik oddal ten glos.
    plusVoteButton.text = comment.plusCount.label
    plusVoteButton.isButtonSelected = comment.plusCount.color != null
    val plusColor =
        comment.plusCount.color?.toColorInt(plusVoteButton.context)
            ?: plusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    plusVoteButton.setTextColor(plusColor)
    plusVoteButton.setOnClick(comment.plusCount.clickAction)

    minusVoteButton.text = comment.minusCount.label
    minusVoteButton.isButtonSelected = comment.minusCount.color != null
    val minusColor =
        comment.minusCount.color?.toColorInt(minusVoteButton.context)
            ?: minusVoteButton.context.readColorAttr(android.R.attr.textColorSecondary)
    minusVoteButton.setTextColor(minusColor)
    minusVoteButton.setOnClick(comment.minusCount.clickAction)

    // Action buttons
    shareTextView.setOnClick(comment.shareAction)
    moreOptionsTextView.setOnClick(comment.moreAction)
    replyTextView.isVisible = onReply != null
    replyTextView.setOnClick(onReply)
    quoteTextView.isVisible = onQuote != null
    quoteTextView.setOnClick(onQuote)
}

// --- Hidden parent comment (using link_comment_layout.xml) ---

internal fun LinkCommentLayoutBinding.bindHiddenCommentV3(
    parent: ParentCommentUi,
    data: LinkCommentUi.Hidden,
) {
    root.setOnClick(data.onClicked)
    root.setOnLongClick(parent.toggleExpansionStateAction)

    // Author
    avatarView.bindAvatarV3(data.author.avatar)
    authorTextView.text = data.author.name
    val authorColor =
        data.author.color?.toColorInt(authorTextView.context)
            ?: authorTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    authorTextView.setTextColor(authorColor)

    // Collapse count
    collapseButtonImageView.isVisible = parent.collapsedCount != null
    collapseButtonImageView.setOnClick(parent.toggleExpansionStateAction)
    collapseButtonImageView.setImageResource(root.context.chevronFor(collapsed = parent.collapsedCount != null))

    // Badge
    val badgeColor = data.badge.toColorInt(context = root.context).defaultColor
    authorBadgeStripView.setBackgroundColor(badgeColor)

    // Ukryty (zablokowany / nowy uzytkownik) komentarz - jak na mikroblogu pokazujemy
    // czytelna zachete zamiast pustego wiersza z samym nickiem. Caly wiersz odslania.
    commentContentTextView.isVisible = true
    commentContentTextView.text = root.context.getString(R.string.hidden_comment_reveal)
    commentContentTextView.setTextColor(root.context.readColorAttr(R.attr.textColorGrey).defaultColor)
    dateTextView.isVisible = false
    dotTextView.isVisible = false
    shareTextView.isVisible = false
    quoteTextView.isVisible = false
    replyTextView.isVisible = false
    plusVoteButton.isVisible = false
    minusVoteButton.isVisible = false
    moreOptionsTextView.isVisible = false
    hideEmbedV3(root)
}

// --- Hidden reply comment (using link_comment_layout.xml) ---

internal fun LinkCommentLayoutBinding.bindHiddenReplyV3(comment: LinkCommentUi.Hidden) {
    root.setOnClick(comment.onClicked)

    // Author
    avatarView.bindAvatarV3(comment.author.avatar)
    authorTextView.text = comment.author.name
    val authorColor =
        comment.author.color?.toColorInt(authorTextView.context)
            ?: authorTextView.context.readColorAttr(AppcompatR.attr.colorControlNormal)
    authorTextView.setTextColor(authorColor)

    // Badge
    val badgeColor =
        (
            comment.badge?.toColorInt(context = root.context)
                ?: root.context.readColorAttr(BaseR.attr.colorDivider)
        ).defaultColor
    authorBadgeStripView.setBackgroundColor(badgeColor)

    // Ukryty (zablokowany / nowy uzytkownik) komentarz - czytelna zachete jak na mikroblogu.
    commentContentTextView.isVisible = true
    commentContentTextView.text = root.context.getString(R.string.hidden_comment_reveal)
    commentContentTextView.setTextColor(root.context.readColorAttr(R.attr.textColorGrey).defaultColor)
    dateTextView.isVisible = false
    collapseButtonImageView.isVisible = false
    dotTextView.isVisible = false
    shareTextView.isVisible = false
    quoteTextView.isVisible = false
    replyTextView.isVisible = false
    plusVoteButton.isVisible = false
    minusVoteButton.isVisible = false
    moreOptionsTextView.isVisible = false
    hideEmbedV3(root)
}

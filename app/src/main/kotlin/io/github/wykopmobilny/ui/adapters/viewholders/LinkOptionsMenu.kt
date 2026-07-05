package io.github.wykopmobilny.ui.adapters.viewholders

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.databinding.LinkBuryMenuBottomsheetBinding
import io.github.wykopmobilny.databinding.LinkMenuBottomsheetBinding
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.ui.fragments.link.LinkInteractor
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.getActivityContext
import io.github.wykopmobilny.utils.loadImage
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.github.wykopmobilny.ui.base.android.R as BaseR

private const val VOTERS_PREVIEW_LIMIT = 12

/**
 * Wspolne menu kontekstowe znaleziska ("..."), uzywane na listach i w szczegolach.
 * Pod pozycjami list glosujacych laduje podglad avatarow (pierwsza strona z API).
 * Opcja zakopania pojawia sie tylko tam, gdzie jest obsluga zakopu (onBury != null).
 */
internal fun openLinkOptionsMenu(
    anchor: View,
    link: Link,
    navigator: NewNavigator,
    linksApi: LinksApi,
    onBury: ((Link, Int) -> Unit)? = null,
) {
    val activityContext = anchor.getActivityContext()!!
    val dialog = BottomSheetDialog(activityContext)
    val bottomSheetView = LinkMenuBottomsheetBinding.inflate(activityContext.layoutInflater)
    dialog.setContentView(bottomSheetView.root)

    val disposables = CompositeDisposable()
    dialog.setOnDismissListener { disposables.dispose() }

    val behavior = BottomSheetBehavior.from(bottomSheetView.root.parent as View)
    // Podglady avatarow doladowuja sie po pokazaniu arkusza - bez ponownego
    // pomiaru peekHeight nowe wiersze bylyby przyciete na dole ekranu.
    fun refreshPeekHeight() {
        bottomSheetView.root.post {
            behavior.peekHeight = bottomSheetView.root.height
        }
    }

    bottomSheetView.apply {
        tvDiggerList.text = root.resources.getString(R.string.dig_list, link.voteCount)
        tvBuryList.text = root.resources.getString(R.string.bury_list, link.buryCount)
        linkDiggers.setOnClickListener {
            navigator.openLinkUpvotersActivity(link.id)
            dialog.dismiss()
        }

        linkBuryList.setOnClickListener {
            navigator.openLinkDownvotersActivity(link.id)
            dialog.dismiss()
        }

        linkBury.isVisible = onBury != null
        linkBury.setOnClickListener {
            dialog.dismiss()
            if (onBury != null) {
                openLinkBuryReasonMenu(anchor, link, onBury)
            }
        }

        linkRelated.setOnClickListener {
            navigator.openLinkRelatedActivity(link.id)
            dialog.dismiss()
        }

        linkReport.isVisible = link.violationUrl != null
        linkReport.setOnClickListener {
            navigator.openReportScreen(link.violationUrl.let(::checkNotNull))
            dialog.dismiss()
        }

        disposables.add(
            linksApi
                .getUpvoters(link.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { upvoters ->
                        diggersPreviewScroll.isVisible = upvoters.isNotEmpty()
                        bindVotersPreview(diggersPreview, upvoters.map { it.author }, navigator, dialog)
                        refreshPeekHeight()
                    },
                    { /* podglad jest opcjonalny - pozycja menu dziala dalej */ },
                ),
        )
        disposables.add(
            linksApi
                .getDownvoters(link.id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { downvoters ->
                        buryListPreviewScroll.isVisible = downvoters.isNotEmpty()
                        bindVotersPreview(buryListPreview, downvoters.map { it.author }, navigator, dialog)
                        refreshPeekHeight()
                    },
                    { /* podglad jest opcjonalny - pozycja menu dziala dalej */ },
                ),
        )
    }

    dialog.setOnShowListener {
        behavior.peekHeight = bottomSheetView.root.height
    }
    dialog.show()
}

private fun bindVotersPreview(
    container: LinearLayout,
    voters: List<Author>,
    navigator: NewNavigator,
    dialog: BottomSheetDialog,
) {
    container.removeAllViews()
    val density = container.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val margin = (4 * density).toInt()
    voters.take(VOTERS_PREVIEW_LIMIT).forEach { author ->
        val avatar = ImageView(container.context)
        avatar.layoutParams =
            LinearLayout.LayoutParams(size, size).apply {
                marginEnd = margin
            }
        if (author.avatarUrl.isBlank()) {
            avatar.setImageResource(BaseR.drawable.avatar)
        } else {
            avatar.loadImage(author.avatarUrl, renderParams = "q80")
        }
        avatar.setOnClickListener {
            dialog.dismiss()
            navigator.openProfileActivity(author.nick)
        }
        container.addView(avatar)
    }
}

internal fun openLinkBuryReasonMenu(
    anchor: View,
    link: Link,
    onBuryReason: (Link, Int) -> Unit,
) {
    val activityContext = anchor.getActivityContext()!!
    val dialog = BottomSheetDialog(activityContext)
    val bottomSheetView = LinkBuryMenuBottomsheetBinding.inflate(activityContext.layoutInflater)
    dialog.setContentView(bottomSheetView.root)

    bottomSheetView.apply {
        reasonDuplicate.setOnClickListener {
            onBuryReason(link, LinkInteractor.BURY_REASON_DUPLICATE)
            dialog.dismiss()
        }

        reasonSpam.setOnClickListener {
            onBuryReason(link, LinkInteractor.BURY_REASON_SPAM)
            dialog.dismiss()
        }

        reasonFakeInfo.setOnClickListener {
            onBuryReason(link, LinkInteractor.BURY_REASON_FAKE_INFO)
            dialog.dismiss()
        }

        reasonWrongContent.setOnClickListener {
            onBuryReason(link, LinkInteractor.BURY_REASON_WRONG_CONTENT)
            dialog.dismiss()
        }

        reasonUnsuitableContent.setOnClickListener {
            onBuryReason(link, LinkInteractor.BURY_REASON_UNSUITABLE_CONTENT)
            dialog.dismiss()
        }
    }

    val behavior = BottomSheetBehavior.from(bottomSheetView.root.parent as View)
    dialog.setOnShowListener {
        behavior.peekHeight = bottomSheetView.root.height
    }
    dialog.show()
}

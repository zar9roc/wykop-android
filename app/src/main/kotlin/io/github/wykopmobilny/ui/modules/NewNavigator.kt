package io.github.wykopmobilny.ui.modules

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.models.dataclass.Embed
import io.github.wykopmobilny.models.dataclass.Link
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.modules.addlink.AddlinkActivity
import io.github.wykopmobilny.ui.modules.embedview.EmbedViewActivity
import io.github.wykopmobilny.ui.modules.input.BaseInputActivity
import io.github.wykopmobilny.ui.modules.report.ReportWebViewActivity
import io.github.wykopmobilny.ui.modules.input.entry.add.AddEntryActivity
import io.github.wykopmobilny.ui.modules.input.entry.comment.EditEntryCommentActivity
import io.github.wykopmobilny.ui.modules.input.entry.edit.EditEntryActivity
import io.github.wykopmobilny.ui.modules.input.link.edit.LinkCommentEditActivity
import io.github.wykopmobilny.ui.modules.links.downvoters.DownvotersActivity
import io.github.wykopmobilny.ui.modules.links.linkdetails.LinkDetailsActivityV2
import io.github.wykopmobilny.ui.modules.links.relatedlinks.RelatedLinksActivity
import io.github.wykopmobilny.ui.modules.links.upvoters.UpvotersActivity
import io.github.wykopmobilny.ui.modules.loginscreen.LoginScreenActivity
import io.github.wykopmobilny.ui.modules.mainnavigation.MainNavigationActivity
import io.github.wykopmobilny.ui.modules.mikroblog.entry.v2.EntryActivityV2
import io.github.wykopmobilny.ui.modules.notificationslist.NotificationsListActivity
import io.github.wykopmobilny.ui.modules.photoview.PhotoViewActivity
import io.github.wykopmobilny.ui.modules.pm.conversation.ConversationActivity
import io.github.wykopmobilny.ui.modules.profile.ProfileActivity
import io.github.wykopmobilny.ui.modules.settings.SettingsActivity
import io.github.wykopmobilny.ui.modules.tag.TagActivity
import io.github.wykopmobilny.utils.openBrowser
import javax.inject.Inject

class NewNavigator
    @Inject
    constructor(
        private val context: Activity,
        private val settingsPreferences: SettingsPreferencesApi,
    ) {
        companion object {
            const val STARTED_FROM_NOTIFICATIONS_CODE = 228
        }

        fun openMainActivity(targetFragment: String? = null) {
            context.startActivity(
                MainNavigationActivity
                    .getIntent(context, targetFragment)
                    .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK) },
            )
        }

        // isRevealed celowo ignorowane w V2 - stan odsłonięcia embeda i tak jest
        // per-obiekt Embed, a mapowanie na ekranie następuje od nowa.
        fun openEntryDetailsActivity(
            entryId: Long,
            @Suppress("UNUSED_PARAMETER") isRevealed: Boolean,
        ) = context.startActivity(EntryActivityV2.createIntent(context, entryId))

        fun openTagActivity(tag: String) = context.startActivity(TagActivity.createIntent(context, tag))

        fun openConversationListActivity(user: String) = context.startActivity(ConversationActivity.createIntent(context, user))

        fun openPhotoViewActivity(url: String) = context.startActivity(PhotoViewActivity.createIntent(context, url))

        fun openSettingsActivity() = context.startActivity(SettingsActivity.createIntent(context))

        fun openLoginScreen() = context.startActivity(LoginScreenActivity.createIntent(context))

        fun openAddEntryActivity(
            receiver: String? = null,
            extraBody: String? = null,
        ) = context.startActivity(AddEntryActivity.createIntent(context, receiver, extraBody))

        fun openEditEntryActivity(
            body: String,
            entryId: Long,
            embed: Embed?,
        ) = context.startActivityForResult(
            EditEntryActivity.createIntent(
                context = context,
                body = body,
                entryId = entryId,
                embed = embed,
            ),
            BaseInputActivity.EDIT_ENTRY,
        )

        fun openEditLinkCommentActivity(
            commentId: Long,
            body: String,
            linkId: Long,
        ) = context.startActivityForResult(
            LinkCommentEditActivity.createIntent(context, commentId, body, linkId),
            BaseInputActivity.EDIT_LINK_COMMENT,
        )

        fun openEditEntryCommentActivity(
            body: String,
            entryId: Long,
            commentId: Long,
            embed: Embed?,
        ) = context.startActivityForResult(
            EditEntryCommentActivity.createIntent(context, body, entryId, commentId, embed),
            BaseInputActivity.EDIT_ENTRY_COMMENT,
        )

        fun openBrowser(url: String) {
            if (settingsPreferences.useBuiltInBrowser) {
                context.openBrowser(url)
            } else {
                val intent =
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(url)
                    }
                context.startActivity(intent)
            }
        }

        // WebView z sesją logowania zamiast przeglądarki - v3 nie ma endpointu
        // zgłoszeń, użytkownik zgłasza przez interfejs strony (zalogowany).
        fun openReportScreen(violationUrl: String) = context.startActivity(ReportWebViewActivity.createIntent(context, violationUrl))

        fun openLinkDetailsActivity(link: Link) = context.startActivity(LinkDetailsActivityV2.createIntent(context, link.id))

        fun openLinkUpvotersActivity(linkId: Long) = context.startActivity(UpvotersActivity.createIntent(linkId, context))

        fun openLinkDetailsActivity(
            linkId: Long,
            commentId: Long = -1L,
        ) = context.startActivity(LinkDetailsActivityV2.createIntent(context, linkId, if (commentId != -1L) commentId else null))

        fun openLinkDownvotersActivity(linkId: Long) = context.startActivity(DownvotersActivity.createIntent(linkId, context))

        // Nowy ekran powiazanych (v3: miniatury, pelny adres, natywne otwieranie).
        fun openLinkRelatedActivity(linkId: Long) = context.startActivity(RelatedLinksActivity.createIntent(context, linkId))

        fun openProfileActivity(username: String) = context.startActivity(ProfileActivity.createIntent(context, username))

        fun openNotificationsListActivity(preselectIndex: Int = NotificationsListActivity.PRESELECT_NOTIFICATIONS) =
            context.startActivityForResult(NotificationsListActivity.createIntent(context, preselectIndex), STARTED_FROM_NOTIFICATIONS_CODE)

        fun openEmbedActivity(url: String) = context.startActivity(EmbedViewActivity.createIntent(context, url))

        // YouTube Player (zamkniete API) usuniete - filmy otwieramy zewnetrznie (przegladarka / aplikacja YouTube).
        fun openYoutubeActivity(url: String) = openBrowser(url)

        fun openAddLinkActivity() = context.startActivity(AddlinkActivity.createIntent(context))

        fun shareUrl(url: String) {
            ShareCompat
                .IntentBuilder(context)
                .setType("text/plain")
                .setChooserTitle(R.string.share)
                .setText(url)
                .startChooser()
        }

    }

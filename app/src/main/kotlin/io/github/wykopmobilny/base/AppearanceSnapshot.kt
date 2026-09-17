package io.github.wykopmobilny.base

import android.content.res.Resources
import io.github.wykopmobilny.R
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi

/**
 * Migawka ustawien wygladu, ktore sa "zapiekane" przy tworzeniu ekranu: rozmiar
 * czcionki wchodzi w motyw (initTheme), a reszta jest cache'owana przez adaptery
 * (`by lazy { settingsPreferencesApi... }`) i decyduje o typach widokow w
 * RecyclerView. Zmiana takiego ustawienia nie odswieza otwartych ekranow - dlatego
 * po powrocie na ekran porownujemy migawke i przy roznicy odtwarzamy Activity.
 *
 * Nowe ustawienie wplywajace na juz zbudowane widoki dopisz tutaj, inaczej zacznie
 * dzialac dopiero po ubiciu aplikacji.
 */
internal data class AppearanceSnapshot(
    val fontSize: String?,
    val linkImagePosition: String,
    val linkShowImage: Boolean,
    val linkSimpleList: Boolean,
    val linkShowAuthor: Boolean,
    val showMinifiedImages: Boolean,
    val cutImages: Boolean,
    val cutImageProportion: Int?,
    val cutLongEntries: Boolean,
    val openSpoilersDialog: Boolean,
    val showTopComments: Boolean,
    val hideBlacklistedViews: Boolean,
    val hideLowRangeAuthors: Boolean,
    val hideContentWithoutTags: Boolean,
    val showAdultContent: Boolean,
    val hideNsfw: Boolean,
    val enableYoutubePlayer: Boolean,
    val enableEmbedPlayer: Boolean,
    val autoplayGifs: Boolean,
    val hideLinkCommentsByDefault: Boolean,
)

internal fun SettingsPreferencesApi.appearanceSnapshot() =
    AppearanceSnapshot(
        fontSize = fontSize,
        linkImagePosition = linkImagePosition,
        linkShowImage = linkShowImage,
        linkSimpleList = linkSimpleList,
        linkShowAuthor = linkShowAuthor,
        showMinifiedImages = showMinifiedImages,
        cutImages = cutImages,
        cutImageProportion = cutImageProportion,
        cutLongEntries = cutLongEntries,
        openSpoilersDialog = openSpoilersDialog,
        showTopComments = showTopComments,
        hideBlacklistedViews = hideBlacklistedViews,
        hideLowRangeAuthors = hideLowRangeAuthors,
        hideContentWithoutTags = hideContentWithoutTags,
        showAdultContent = showAdultContent,
        hideNsfw = hideNsfw,
        enableYoutubePlayer = enableYoutubePlayer,
        enableEmbedPlayer = enableEmbedPlayer,
        autoplayGifs = autoplayGifs,
        hideLinkCommentsByDefault = hideLinkCommentsByDefault,
    )

/** Nakladka z rozmiarem czcionki - wspolna dla obu baz Activity (stary i nowy stack). */
internal fun Resources.Theme.applyFontSize(fontSize: String?) {
    val style =
        when (fontSize) {
            "tiny" -> R.style.TextSizeTiny
            "small" -> R.style.TextSizeSmall
            "large" -> R.style.TextSizeLarge
            "huge" -> R.style.TextSizeHuge
            else -> R.style.TextSizeNormal
        }
    applyStyle(style, true)
}

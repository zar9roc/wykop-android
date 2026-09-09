package io.github.wykopmobilny.ui.modules.input

import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.base.BasePresenter

interface BaseInputPresenter {
    fun sendWithPhoto(
        photo: WykopImageFile,
        containsAdultContent: Boolean,
        embedUrl: String? = null,
    )

    fun sendWithPhotoUrl(
        photo: String?,
        containsAdultContent: Boolean,
        embedUrl: String? = null,
    )
}

@Suppress("UnnecessaryAbstractClass")
abstract class InputPresenter<T : BaseInputView> :
    BasePresenter<T>(),
    BaseInputPresenter

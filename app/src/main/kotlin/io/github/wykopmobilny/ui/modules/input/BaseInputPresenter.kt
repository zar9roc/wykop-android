package io.github.wykopmobilny.ui.modules.input

import io.github.wykopmobilny.api.PhotoSource
import io.github.wykopmobilny.base.BasePresenter

interface BaseInputPresenter {
    /** [photos] w kolejnosci zalaczania; pusta lista = sama tresc (i ewentualny embed). */
    fun send(
        photos: List<PhotoSource>,
        containsAdultContent: Boolean,
        embedUrl: String? = null,
    )
}

@Suppress("UnnecessaryAbstractClass")
abstract class InputPresenter<T : BaseInputView> :
    BasePresenter<T>(),
    BaseInputPresenter

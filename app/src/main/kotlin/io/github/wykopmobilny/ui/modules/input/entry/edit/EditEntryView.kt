package io.github.wykopmobilny.ui.modules.input.entry.edit

import io.github.wykopmobilny.ui.modules.input.BaseInputView

interface EditEntryView : BaseInputView {
    val entryId: Long

    /** Ankieta wpisu - edycja musi ja odeslac, inaczej API ja usunie. */
    val surveyKey: String?
}

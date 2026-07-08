package io.github.wykopmobilny.ui.widgets

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.wykopmobilny.R
import io.github.wykopmobilny.WykopApp
import io.github.wykopmobilny.ui.dialogs.noteDialog
import io.github.wykopmobilny.utils.getActivityContext
import kotlinx.coroutines.launch
import io.github.wykopmobilny.ui.base.android.R as BaseR

/**
 * Mala "zolta kartka" tuz za nickiem (compound drawable na koncu TextView) - sygnalizuje,
 * ze zalogowany user ma notatke o autorze. Ikona monochromatyczna (ic_note) wybarwiona
 * na zolto przez tint (zgodnie z regula ikon w CLAUDE.md). Brak zmian w layoutach.
 */
fun TextView.setNoteCard(hasNote: Boolean) {
    val drawable =
        if (hasNote) {
            val sizePx = (NOTE_CARD_DP * resources.displayMetrics.density).toInt()
            ContextCompat.getDrawable(context, BaseR.drawable.ic_note)?.mutate()?.apply {
                setBounds(0, 0, sizePx, sizePx)
                setTint(ContextCompat.getColor(context, io.github.wykopmobilny.R.color.note_card))
            }
        } else {
            null
        }
    setCompoundDrawablesRelative(null, null, drawable, null)
    compoundDrawablePadding = (NOTE_CARD_PADDING_DP * resources.displayMetrics.density).toInt()
}

private const val NOTE_CARD_DP = 15
private const val NOTE_CARD_PADDING_DP = 4

/**
 * Podpina pierwsza pozycje "notatka" w menu "..." (bottom-sheet). Etykieta = tresc notatki
 * kursywa (dociagana z API) lub "Dodaj notatke" gdy brak. Klik otwiera popup edycji;
 * po zapisie/usunieciu wola [onChanged] z nowa flaga (do odswiezenia zoltej kartki).
 */
fun bindNoteMenuItem(
    item: View,
    label: TextView,
    nick: String,
    dismissMenu: () -> Unit,
    onChanged: (Boolean) -> Unit,
) {
    val context = item.context
    val repo = (context.applicationContext as WykopApp).notesRepository.get()
    val scope = (item.getActivityContext() as? LifecycleOwner)?.lifecycleScope

    // Zawsze dociagamy aktualna tresc z API (flaga z payloadu bywa nieaktualna po edycji).
    label.setText(R.string.note_menu_add)
    label.setTypeface(null, Typeface.NORMAL)
    scope?.launch {
        val content = runCatching { repo.getNote(nick) }.getOrNull()
        if (!content.isNullOrBlank()) {
            label.text = content
            label.setTypeface(null, Typeface.ITALIC)
        }
    }

    item.setOnClickListener {
        dismissMenu()
        scope?.launch {
            val content = runCatching { repo.getNote(nick) }.getOrNull()
            noteDialog(context, content) { newText ->
                scope.launch {
                    runCatching { repo.saveNote(nick, newText) }
                        .onSuccess { onChanged(newText.isNotBlank()) }
                }
            }.show()
        }
    }
}

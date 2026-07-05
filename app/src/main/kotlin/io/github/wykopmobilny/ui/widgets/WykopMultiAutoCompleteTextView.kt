package io.github.wykopmobilny.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.MultiAutoCompleteTextView

/**
 * MultiAutoCompleteTextView bez cofania całego tokenu backspace'em.
 *
 * Oryginalny [MultiAutoCompleteTextView.replaceText] oznacza wstawiony tekst przez
 * QwertyKeyListener.markAsReplaced() - pierwszy backspace po autouzupełnieniu
 * przywraca wtedy wpisany prefiks, kasując cały podpowiedziany tag/login naraz.
 * Ta wersja robi tę samą podmianę tokenu, tylko bez markera "replaced",
 * więc backspace kasuje pojedyncze znaki.
 */
class WykopMultiAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : MultiAutoCompleteTextView(context, attrs) {
    private var tokenizer: Tokenizer? = null

    override fun setTokenizer(t: Tokenizer?) {
        tokenizer = t
        super.setTokenizer(t)
    }

    override fun replaceText(text: CharSequence) {
        val tokenizer = tokenizer ?: return super.replaceText(text)
        clearComposingText()
        val end = selectionEnd
        val start = tokenizer.findTokenStart(getText(), end)
        getText().replace(start, end, tokenizer.terminateToken(text))
    }
}

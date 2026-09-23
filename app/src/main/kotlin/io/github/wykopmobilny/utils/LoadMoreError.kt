package io.github.wykopmobilny.utils

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R

/**
 * Nieudane doladowanie kolejnej strony listy. Zamiast modalnego dialogu (ktory
 * przykrywa juz wczytana tresc i nie daje sie ponowic) pokazuje snackbar z akcja
 * "Ponow". Blad i tak trafia do logu.
 */
fun Fragment.showLoadMoreErrorSnackbar(
    throwable: Throwable,
    onRetry: () -> Unit,
) {
    Napier.w("Nie udalo sie doladowac kolejnej strony: ${throwable.message}")
    val root = view ?: return
    Snackbar
        .make(root, R.string.load_more_failed, Snackbar.LENGTH_LONG)
        .setAction(R.string.load_more_retry) { onRetry() }
        .show()
}

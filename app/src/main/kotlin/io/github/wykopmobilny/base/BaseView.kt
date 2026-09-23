package io.github.wykopmobilny.base

interface BaseView {
    fun showErrorDialog(e: Throwable)

    /**
     * Blad doladowania kolejnej strony przy scrollu - w odroznieniu od pierwszego
     * wczytania nie powinien przykrywac modalem tresci, ktora juz sie wyswietla.
     * Domyslnie zachowuje sie jak [showErrorDialog]; ekrany z nieskonczona lista
     * nadpisuja to nieblokujacym komunikatem z opcja ponowienia.
     */
    fun showLoadMoreError(e: Throwable) = showErrorDialog(e)
}

package io.github.wykopmobilny.domain.errorhandling

sealed class KnownError : Throwable() {
    data class TwoFactorAuthorizationRequired(
        override val message: String,
    ) : KnownError()

    /** Tresc usunieta albo nieistniejaca (HTTP 404) - czytelny komunikat zamiast surowego bledu. */
    data class ContentNotFound(
        override val message: String = "Ta treść została usunięta lub nie istnieje.",
    ) : KnownError()
}

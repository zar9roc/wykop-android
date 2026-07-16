package io.github.wykopmobilny.utils.linkhandler.linkparser

object EntryLinkParser {
    fun getEntryId(url: String): Long? {
        if (url.contains("/wpis/")) {
            return url.substringAfter("/wpis/").substringBefore("/").toLongOrNull()
        }
        return null
    }

    fun getEntryCommentId(url: String): Long? {
        if (url.contains("/#comment-")) {
            return url.substringAfter("/#comment-").substringBefore("/").toLongOrNull()
        }
        // Format sciezkowy ze strony: /wpis/{id}/{slug}/komentarz/{commentId}/
        if (url.contains("/komentarz/")) {
            return url.substringAfter("/komentarz/").substringBefore("/").toLongOrNull()
        }
        // Format z powiadomien: goly fragment #<commentId>
        // (np. /wpis/57976055/slug/strona/448#297797701)
        if (url.contains("#")) {
            return url.substringAfterLast("#").toLongOrNull()
        }
        return null
    }

    // Numer strony komentarzy z URL-a (/wpis/{id}/{slug}/strona/{NNN}) - pozwala
    // ekranowi wpisu startowac od wlasciwej strony zamiast ladowac wszystkie.
    fun getEntryPage(url: String): Int? {
        if (url.contains("/strona/")) {
            return url
                .substringAfter("/strona/")
                .substringBefore("/")
                .substringBefore("#")
                .toIntOrNull()
        }
        return null
    }
}

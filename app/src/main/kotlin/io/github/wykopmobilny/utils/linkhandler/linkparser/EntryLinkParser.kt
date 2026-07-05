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
        return null
    }
}

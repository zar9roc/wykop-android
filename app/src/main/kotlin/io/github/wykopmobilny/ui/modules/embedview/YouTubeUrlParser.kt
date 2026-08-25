package io.github.wykopmobilny.ui.modules.embedview

import java.net.URLDecoder

// Parsowanie adresow YouTube (FOSS, bez zaleznosci od zamknietego YouTube Player API).
// Wlasciwe odtwarzanie odbywa sie zewnetrznie (przegladarka / aplikacja YouTube).
object YouTubeUrlParser {
    private val consentRegex = "consent.youtube.com/.+[?&]continue=([a-z0-9-_.~%]+[^&\\n])".toRegex(RegexOption.IGNORE_CASE)
    private val videoRegex =
        "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/\\S+/|(?:v|e(?:mbed)?)/|\\S*?[?&]v=)|youtu\\.be/)([a-z0-9_-]{11})".toRegex(
            RegexOption.IGNORE_CASE,
        )
    private val timestampRegex = "t=([^#&\\n\\r]+)".toRegex(RegexOption.IGNORE_CASE)

    fun getVideoId(videoUrl: String): String? {
        val unwrappedUrl = unwrapConsentYoutubeUrl(videoUrl)
        return findInUrl(videoRegex, unwrappedUrl)
    }

    fun getTimestamp(videoUrl: String): String? = findInUrl(timestampRegex, videoUrl)

    fun getVideoUrl(videoId: String): String = "http://youtu.be/$videoId"

    fun isVideoUrl(url: String): Boolean = videoRegex.find(unwrapConsentYoutubeUrl(url)) != null

    private fun findInUrl(
        regex: Regex,
        url: String,
    ): String? {
        val match = regex.find(url)
        return match?.groupValues?.get(1)
    }

    private fun unwrapConsentYoutubeUrl(url: String): String {
        val match = consentRegex.find(url) ?: return url
        return URLDecoder.decode(match.groupValues[1], "utf-8")
    }
}

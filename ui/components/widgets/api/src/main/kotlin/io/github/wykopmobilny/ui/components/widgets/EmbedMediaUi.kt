package io.github.wykopmobilny.ui.components.widgets

data class EmbedMediaUi(
    val content: Content,
    val size: String?,
    // null = brak zaslony. Nsfw dla tresci z #nsfw, Plus18 dla adult bez #nsfw.
    val overlay: Overlay?,
    val clickAction: () -> Unit,
    val widthToHeightRatio: Float,
) {
    enum class Overlay { Nsfw, Plus18 }

    sealed class Content {
        data class StaticImage(
            val url: String,
        ) : Content()

        data class PlayableMedia(
            val previewImage: String,
            val domain: String,
        ) : Content()
    }
}

const val NSFW_PLACEHOLDER = "https://www.wykop.pl/cdn/c2526412/nsfw.jpg"
const val PLUS18_PLACEHOLDER = "https://www.wykop.pl/cdn/c2526412/18plus.jpg"

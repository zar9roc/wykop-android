package io.github.wykopmobilny.models.dataclass

data class Related(
    val id: Int,
    val url: String,
    var voteCount: Int,
    val author: Author?,
    val title: String,
    var userVote: Int,
    // Miniatura (wariant w400 z CDN) - null gdy powiazane jej nie ma.
    val previewImageUrl: String? = null,
)

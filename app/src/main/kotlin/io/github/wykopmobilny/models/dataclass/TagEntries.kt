package io.github.wykopmobilny.models.dataclass

data class TagEntries(
    val entries: List<Entry>,
    val nextPage: String? = null,
)

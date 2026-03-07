package io.github.wykopmobilny.models.dataclass

data class TagLinks(
    val entries: List<Link>,
    val nextPage: String? = null,
)

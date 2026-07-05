package io.github.wykopmobilny.ui.fragments.links

import io.github.wykopmobilny.models.dataclass.Link

interface LinkActionListener {
    fun dig(link: Link)

    fun bury(
        link: Link,
        reason: Int,
    )

    fun removeVote(link: Link)
}

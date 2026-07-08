package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.mapper.Mapper
import io.github.wykopmobilny.utils.api.colorNameToGroupId

object AuthorMapperV3 : Mapper<UserShortResponseV3, Author> {
    override fun map(value: UserShortResponseV3) =
        Author(
            value.username,
            value.avatar.orEmpty(),
            // Zbanowani/usunieci maja w `color` dalej rangowy kolor - status
            // decyduje o szarym nicku (grupy 1001/1002 = #999999 w getGroupColor).
            when (value.status) {
                "banned" -> GROUP_ID_BANNED
                "removed" -> GROUP_ID_DELETED
                else -> colorNameToGroupId(value.color)
            },
            value.gender.orEmpty(),
            hasNote = value.note ?: false,
        )

    private const val GROUP_ID_BANNED = 1001
    private const val GROUP_ID_DELETED = 1002
}

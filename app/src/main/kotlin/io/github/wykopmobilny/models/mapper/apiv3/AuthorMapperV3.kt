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
            colorNameToGroupId(value.color),
            value.gender.orEmpty(),
        )
}

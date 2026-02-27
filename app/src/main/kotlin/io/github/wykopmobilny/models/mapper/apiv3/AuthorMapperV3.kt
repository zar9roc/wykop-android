package io.github.wykopmobilny.models.mapper.apiv3

import android.graphics.Color
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.mapper.Mapper

object AuthorMapperV3 : Mapper<UserShortResponseV3, Author> {
    override fun map(value: UserShortResponseV3) =
        Author(
            value.username,
            value.avatar.orEmpty(),
            value.color?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: 0,
            value.gender.orEmpty(),
        )
}

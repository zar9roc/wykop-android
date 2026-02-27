package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import io.github.wykopmobilny.models.dataclass.Voter
import io.github.wykopmobilny.models.mapper.Mapper

object VoterMapperV3 : Mapper<UserShortResponseV3, Voter> {
    override fun map(value: UserShortResponseV3) =
        Voter(
            AuthorMapperV3.map(value),
            "",
            0,
        )
}

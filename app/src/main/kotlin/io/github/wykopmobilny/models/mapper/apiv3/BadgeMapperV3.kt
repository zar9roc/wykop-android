package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.BadgeResponse
import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.models.mapper.Mapper

object BadgeMapperV3 : Mapper<BadgeResponseV3, BadgeResponse> {
    override fun map(value: BadgeResponseV3) =
        BadgeResponse(
            name = value.label.orEmpty(),
            date = value.achievedAt.orEmpty(),
            description = value.description.orEmpty(),
            icon =
                value.media
                    ?.icon
                    ?.url
                    .orEmpty(),
        )
}

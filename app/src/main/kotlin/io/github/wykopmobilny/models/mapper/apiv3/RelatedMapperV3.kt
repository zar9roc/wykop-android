package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.links.RelatedResponseV3
import io.github.wykopmobilny.models.dataclass.Related
import io.github.wykopmobilny.models.mapper.Mapper

object RelatedMapperV3 : Mapper<RelatedResponseV3, Related> {
    override fun map(value: RelatedResponseV3) =
        Related(
            id = value.id.toInt(),
            url = value.url.orEmpty(),
            voteCount = value.votes.up - value.votes.down,
            author = AuthorMapperV3.map(value.author),
            title = value.title.orEmpty(),
            userVote = value.voted ?: 0,
        )
}

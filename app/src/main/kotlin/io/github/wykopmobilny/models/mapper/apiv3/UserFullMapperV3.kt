package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.ProfileResponse
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.models.mapper.Mapper
import kotlinx.datetime.Instant

object UserFullMapperV3 : Mapper<UserFullResponseV3, ProfileResponse> {
    override fun map(value: UserFullResponseV3) =
        ProfileResponse(
            signupAt = runCatching { Instant.parse(value.memberSince.orEmpty()) }.getOrElse { Instant.DISTANT_PAST },
            background = value.background,
            isVerified = value.verified,
            isObserved = value.follow,
            isBlocked = null,
            email = value.publicEmail,
            description = value.about,
            name = value.name,
            wwwUrl = value.website,
            jabberUrl = null,
            ggUrl = null,
            city = value.city,
            facebookUrl = value.socialMedia?.facebook,
            twitterUrl = value.socialMedia?.twitter,
            instagramUrl = value.socialMedia?.instagram,
            linksAddedCount = value.summary?.linksDetails?.added,
            linksPublishedCount = value.summary?.linksDetails?.published,
            commentsCount = value.summary?.linksDetails?.commented,
            rank = value.rank?.position,
            followers = value.summary?.followers,
            following = value.summary?.followingUsers,
            entriesCount = value.summary?.entries,
            entriesCommentsCount = value.summary?.entriesDetails?.commented,
            diggsCount = value.summary?.linksDetails?.up,
            buriesCount = value.summary?.linksDetails?.down,
            violationUrl = null,
            ban =
                value.banned?.let {
                    io.github.wykopmobilny.api.responses.BanResponse(
                        reason = it.reason.orEmpty(),
                        date = it.expired.orEmpty(),
                    )
                },
            id = value.username,
            color =
                value.color
                    ?.hex
                    ?.removePrefix("#")
                    ?.toIntOrNull(16) ?: 0,
            sex = value.gender,
            avatar = value.avatar.orEmpty(),
        )
}

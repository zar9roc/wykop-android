package io.github.wykopmobilny.api.responses.v3.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserFullResponseV3(
    // Fields from UserShort
    @field:Json(name = "username") val username: String,
    @field:Json(name = "company") val company: Boolean?,
    @field:Json(name = "gender") val gender: String?,
    @field:Json(name = "avatar") val avatar: String?,
    @field:Json(name = "note") val note: Boolean?,
    @field:Json(name = "online") val online: Boolean?,
    @field:Json(name = "status") val status: String?,
    @field:Json(name = "color") val color: UserColorResponseV3?,
    @field:Json(name = "verified") val verified: Boolean?,
    @field:Json(name = "follow") val follow: Boolean?,
    @field:Json(name = "rank") val rank: UserRankResponseV3?,
    // Additional fields from UserFull
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "city") val city: String?,
    @field:Json(name = "website") val website: String?,
    @field:Json(name = "about") val about: String?,
    @field:Json(name = "public_email") val publicEmail: String?,
    @field:Json(name = "background") val background: String?,
    @field:Json(name = "followers") val followers: Int?,
    @field:Json(name = "member_since") val memberSince: String?,
    @field:Json(name = "summary") val summary: UserSummaryResponseV3?,
    @field:Json(name = "social_media") val socialMedia: UserSocialMediaResponseV3?,
    @field:Json(name = "banned") val banned: UserBannedResponseV3?,
)

@JsonClass(generateAdapter = true)
data class UserSummaryResponseV3(
    @field:Json(name = "actions") val actions: Int?,
    @field:Json(name = "links") val links: Int?,
    @field:Json(name = "links_details") val linksDetails: UserLinksDetailsResponseV3?,
    @field:Json(name = "entries") val entries: Int?,
    @field:Json(name = "entries_details") val entriesDetails: UserEntriesDetailsResponseV3?,
    @field:Json(name = "following_users") val followingUsers: Int?,
    @field:Json(name = "following_tags") val followingTags: Int?,
    @field:Json(name = "followers") val followers: Int?,
)

@JsonClass(generateAdapter = true)
data class UserLinksDetailsResponseV3(
    @field:Json(name = "added") val added: Int?,
    @field:Json(name = "commented") val commented: Int?,
    @field:Json(name = "published") val published: Int?,
    @field:Json(name = "related") val related: Int?,
    @field:Json(name = "up") val up: Int?,
    @field:Json(name = "down") val down: Int?,
)

@JsonClass(generateAdapter = true)
data class UserEntriesDetailsResponseV3(
    @field:Json(name = "added") val added: Int?,
    @field:Json(name = "commented") val commented: Int?,
    @field:Json(name = "voted") val voted: Int?,
)

@JsonClass(generateAdapter = true)
data class UserSocialMediaResponseV3(
    @field:Json(name = "facebook") val facebook: String?,
    @field:Json(name = "instagram") val instagram: String?,
    @field:Json(name = "twitter") val twitter: String?,
)

@JsonClass(generateAdapter = true)
data class UserBannedResponseV3(
    @field:Json(name = "reason") val reason: String?,
    @field:Json(name = "expired") val expired: String?,
)

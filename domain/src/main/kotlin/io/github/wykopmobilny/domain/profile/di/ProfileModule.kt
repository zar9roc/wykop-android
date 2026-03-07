package io.github.wykopmobilny.domain.profile.di

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.StoreBuilder
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.cache.api.GenderEntity
import io.github.wykopmobilny.data.cache.api.ProfileDetailsView
import io.github.wykopmobilny.data.cache.api.ProfileEntity
import io.github.wykopmobilny.data.cache.api.UserColorEntity
import io.github.wykopmobilny.domain.api.PagingSource
import io.github.wykopmobilny.domain.api.StoreMediator
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.profile.GetProfileActionsQuery
import io.github.wykopmobilny.domain.profile.GetProfileDetailsQuery
import io.github.wykopmobilny.domain.profile.InitializeProfile
import io.github.wykopmobilny.domain.profile.ProfileAction
import io.github.wykopmobilny.domain.profile.ProfileId
import io.github.wykopmobilny.domain.profile.datasource.profileSourceOfTruth
import io.github.wykopmobilny.kotlin.AppDispatchers
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.ui.base.SimpleViewStateStorage
import io.github.wykopmobilny.ui.profile.GetProfileActions
import io.github.wykopmobilny.ui.profile.GetProfileDetails
import kotlinx.datetime.Instant
import javax.inject.Provider

@Module
internal abstract class ProfileModule {
    companion object {
        @ProfileScope
        @Provides
        fun viewState() = SimpleViewStateStorage()

        @ProfileScope
        @Provides
        fun actionsStore(
            @ProfileId profileId: String,
            profileApiV3: ProfileV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
        ): Store<Int, List<ProfileAction>> =
            StoreBuilder
                .from(
                    fetcher =
                        Fetcher.of { page: Int ->
                            profileApiV3.getUserActions(profileId, page).data.orEmpty()
                        },
                    sourceOfTruth = profileSourceOfTruth(profileId, cache),
                ).scope(appScopes.applicationScope)
                .build()

        @Provides
        fun actionsPager(
            mediator: StoreMediator<ProfileAction>,
            pagingSource: Provider<PagingSource<ProfileAction>>,
        ): Pager<Int, ProfileAction> =
            Pager(
                config = PagingConfig(pageSize = 20),
                remoteMediator = mediator,
                pagingSourceFactory = pagingSource::get,
            )

        @ProfileScope
        @Provides
        fun profileStore(
            @ProfileId profileId: String,
            profileApiV3: ProfileV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
        ): Store<Unit, ProfileDetailsView> =
            StoreBuilder
                .from(
                    fetcher =
                        Fetcher.of {
                            val response = profileApiV3.getUserProfile(profileId)
                            response.data ?: error("No profile data for $profileId")
                        },
                    sourceOfTruth =
                        SourceOfTruth.of<Unit, UserFullResponseV3, ProfileDetailsView>(
                            reader = {
                                cache.profileQueries
                                    .selectById(profileId)
                                    .asFlow()
                                    .mapToOneOrNull(AppDispatchers.IO)
                            },
                            writer = { _, input ->
                                cache.transaction {
                                    if (input.follow == true) {
                                        cache.profileStateQueries.observeProfile(input.username)
                                    } else {
                                        cache.profileStateQueries.unobserveProfile(input.username)
                                    }
                                    cache.profileQueries.insertOrReplace(input.toProfileEntity())
                                }
                            },
                            delete = { cache.profileQueries.deleteById(profileId) },
                            deleteAll = { cache.profileQueries.deleteAll() },
                        ),
                ).scope(appScopes.applicationScope)
                .build()
    }

    @Binds
    abstract fun getProfileDetails(impl: GetProfileDetailsQuery): GetProfileDetails

    @Binds
    abstract fun getProfileEntries(impl: GetProfileActionsQuery): GetProfileActions

    @Binds
    abstract fun scopeInitializer(impl: InitializeProfile): ScopeInitializer
}

private fun UserFullResponseV3.toProfileEntity() =
    ProfileEntity(
        id = username,
        signupAt = runCatching { Instant.parse(memberSince.orEmpty()) }.getOrElse { Instant.DISTANT_PAST },
        background = background,
        isVerified = verified == true,
        email = publicEmail,
        description = about,
        name = name,
        wwwUrl = website,
        jabberUrl = null,
        ggUrl = null,
        city = city,
        facebookUrl = socialMedia?.facebook,
        twitterUrl = socialMedia?.twitter,
        instagramUrl = socialMedia?.instagram,
        linksAddedCount = summary?.linksDetails?.added,
        linksPublishedCount = summary?.linksDetails?.published,
        commentsCount = summary?.linksDetails?.commented,
        rank = rank?.position,
        followers = summary?.followers,
        following = summary?.followingUsers,
        entriesCount = summary?.entries,
        entriesCommentsCount = summary?.entriesDetails?.commented,
        diggsCount = summary?.linksDetails?.up,
        buriesCount = summary?.linksDetails?.down,
        violationUrl = null,
        banReason = banned?.reason,
        banDate = banned?.expired,
        color = color.toColorEntityFromName(),
        gender = gender.toGenderEntity(),
        avatar = avatar.orEmpty(),
    )

@Suppress("MagicNumber")
internal fun Int.toColorEntity() =
    when (this) {
        0 -> UserColorEntity.Green
        1 -> UserColorEntity.Orange
        2 -> UserColorEntity.Claret
        5 -> UserColorEntity.Admin
        1001 -> UserColorEntity.Banned
        1002 -> UserColorEntity.Deleted
        2001 -> UserColorEntity.Client
        else -> UserColorEntity.Unknown
    }

internal fun String?.toGenderEntity() =
    when (this) {
        "male" -> GenderEntity.Male
        "female" -> GenderEntity.Female
        else -> null
    }

internal fun String?.toColorEntityFromName() =
    when (this) {
        "green" -> UserColorEntity.Green
        "orange" -> UserColorEntity.Orange
        "burgundy" -> UserColorEntity.Claret
        "black" -> UserColorEntity.Admin
        else -> UserColorEntity.Unknown
    }

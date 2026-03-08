package io.github.wykopmobilny.domain.di

import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dagger.Module
import dagger.Provides
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.api.ErrorBodyParser
import io.github.wykopmobilny.api.endpoints.LoginRetrofitApi
import io.github.wykopmobilny.api.responses.LoginResponse
import io.github.wykopmobilny.blacklist.api.ScraperRetrofitApi
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.domain.api.apiCall
import io.github.wykopmobilny.storage.api.Blacklist
import io.github.wykopmobilny.storage.api.LoggedUserInfo
import io.github.wykopmobilny.storage.api.UserInfoStorage
import io.github.wykopmobilny.storage.api.UserSession
import io.github.wykopmobilny.kotlin.AppDispatchers
import io.github.wykopmobilny.kotlin.AppScopes
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Singleton

@Module
internal class StoresModule {
    @Singleton
    @Provides
    fun blacklistStore(
        retrofitApi: ScraperRetrofitApi,
        storage: AppStorage,
        appScopes: AppScopes,
    ) = StoreBuilder
        .from<Unit, Blacklist, Blacklist>(
            fetcher =
                Fetcher.of {
                    val api = retrofitApi.getBlacklist()
                    Blacklist(
                        tags =
                            api.tags
                                ?.tags
                                ?.mapNotNull { it.tag?.removePrefix("#") }
                                ?.toSet()
                                .orEmpty(),
                        users =
                            api.users
                                ?.users
                                ?.mapNotNull { it.nick?.removePrefix("@") }
                                ?.toSet()
                                .orEmpty(),
                    )
                },
            sourceOfTruth =
                flowSourceOfTruth(
                    reader = {
                        combine(
                            storage.blacklistQueries
                                .allTags()
                                .asFlow()
                                .mapToList(AppDispatchers.Default),
                            storage.blacklistQueries
                                .allProfiles()
                                .asFlow()
                                .mapToList(AppDispatchers.Default),
                        ) { tags, profiles ->
                            Blacklist(
                                tags = tags.toSet(),
                                users = profiles.toSet(),
                            )
                        }
                    },
                    writer = { _, newValue ->
                        storage.blacklistQueries.transaction {
                            storage.blacklistQueries.deleteAll()
                            newValue.tags.forEach(storage.blacklistQueries::insertOrReplaceTag)
                            newValue.users.forEach(storage.blacklistQueries::insertOrReplaceProfile)
                        }
                    },
                    delete = { error("unsupported") },
                    deleteAll = {
                        storage.blacklistQueries.transaction {
                            storage.blacklistQueries.deleteAll()
                        }
                    },
                ),
        ).scope(appScopes.applicationScope)
        .build()

    @Singleton
    @Provides
    fun loginStore(
        retrofitApi: LoginRetrofitApi,
        storage: UserInfoStorage,
        appScopes: AppScopes,
        errorBodyParser: ErrorBodyParser,
    ): Store<UserSession, LoggedUserInfo> = StoreBuilder
        .from(
            fetcher =
                Fetcher.ofResult { request: UserSession ->
                    apiCall(
                        errorBodyParser = errorBodyParser,
                        onTwoFactorAuthorizationRequired = { Napier.e("2FA not handled") },
                        rawCall = { retrofitApi.getUserSessionToken(login = request.login, accountKey = request.token) },
                        onUnauthorized = null,
                    )
                },
            sourceOfTruth =
                flowSourceOfTruth(
                    reader = { storage.loggedUser.filterNotNull() },
                    writer = { _, newValue -> storage.updateLoggedUser(newValue.toLoggedUserInfo()) },
                    delete = { storage.updateLoggedUser(null) },
                    deleteAll = { storage.updateLoggedUser(null) },
                ),
        ).scope(appScopes.applicationScope)
        .build()
}

fun LoginResponse.toLoggedUserInfo() =
    LoggedUserInfo(
        id = profile.id,
        userToken = userkey,
        avatarUrl = profile.avatar,
        backgroundUrl = profile.background,
    )

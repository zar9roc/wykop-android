package io.github.wykopmobilny.domain.repositories

import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.blacklist.BlacklistUserRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.storage.api.AppStorage
import javax.inject.Inject

internal class ProfilesRepository
    @Inject
    constructor(
        private val profilesApiV3: ProfileV3RetrofitApi,
        private val appCache: AppCache,
        private val appStorage: AppStorage,
    ) {
        suspend fun blockUser(profileId: String) {
            // Blokada przez kolekcje POST /users {data:{username}} (sciezka /users/{username} = 405).
            profilesApiV3.blockUser(WykopApiRequestV3(BlacklistUserRequestV3(username = profileId.removePrefix("@"))))
            appStorage.blacklistQueries.insertOrReplaceProfile(profileId)
            appCache.profileStateQueries.blockProfile(profileId)
        }

        suspend fun unblockUser(profileId: String) {
            profilesApiV3.unblockUser(profileId)
            appStorage.blacklistQueries.deleteProfile(profileId)
            appCache.profileStateQueries.unblockProfile(profileId)
        }

        suspend fun observeUser(profileId: String) {
            profilesApiV3.observeUser(profileId)
            appCache.profileStateQueries.observeProfile(profileId)
        }

        suspend fun unobserveUser(profileId: String) {
            profilesApiV3.unobserveUser(profileId)
            appCache.profileStateQueries.unobserveProfile(profileId)
        }
    }

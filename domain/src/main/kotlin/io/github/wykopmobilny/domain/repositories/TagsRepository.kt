package io.github.wykopmobilny.domain.repositories

import io.github.wykopmobilny.api.endpoints.v3.TagsV3RetrofitApi
import io.github.wykopmobilny.api.requests.v3.blacklist.BlacklistTagRequestV3
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.data.storage.api.AppStorage
import javax.inject.Inject

internal class TagsRepository
    @Inject
    constructor(
        private val tagsApiV3: TagsV3RetrofitApi,
        private val appStorage: AppStorage,
    ) {
        suspend fun blockTag(tag: String) {
            // Blokada przez kolekcje POST /tags {data:{tag}} (sciezka /tags/{tag} = 405).
            tagsApiV3.blockTag(WykopApiRequestV3(BlacklistTagRequestV3(tag = tag.removePrefix("#"))))
            appStorage.blacklistQueries.insertOrReplaceTag(tagId = tag)
        }

        suspend fun unblockTag(tag: String) {
            tagsApiV3.unblockTag(tag)
            appStorage.blacklistQueries.deleteTag(tagId = tag)
        }
    }

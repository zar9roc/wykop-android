package io.github.wykopmobilny.domain.repositories

import io.github.wykopmobilny.api.endpoints.v3.TagsV3RetrofitApi
import io.github.wykopmobilny.data.storage.api.AppStorage
import javax.inject.Inject

internal class TagsRepository
    @Inject
    constructor(
        private val tagsApiV3: TagsV3RetrofitApi,
        private val appStorage: AppStorage,
    ) {
        suspend fun blockTag(tag: String) {
            tagsApiV3.blockTag(tag)
            appStorage.blacklistQueries.insertOrReplaceTag(tagId = tag)
        }

        suspend fun unblockTag(tag: String) {
            tagsApiV3.unblockTag(tag)
            appStorage.blacklistQueries.deleteTag(tagId = tag)
        }
    }

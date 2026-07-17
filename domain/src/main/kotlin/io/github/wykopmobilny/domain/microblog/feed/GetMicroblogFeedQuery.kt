package io.github.wykopmobilny.domain.microblog.feed

import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedKey
import io.github.wykopmobilny.domain.microblog.feed.di.MicroblogFeedScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.domain.utils.withResource
import io.github.wykopmobilny.entries.feed.GetMicroblogFeed
import io.github.wykopmobilny.entries.feed.MicroblogFeedSort
import io.github.wykopmobilny.entries.feed.MicroblogFeedUi
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.ui.base.Resource
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.base.components.SwipeRefreshUi
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class GetMicroblogFeedQuery
    @Inject
    constructor(
        private val key: MicroblogFeedKey,
        private val storage: MicroblogFeedStateStorage,
        private val pager: EntryFeedPager,
        private val appScopes: AppScopes,
    ) : GetMicroblogFeed {
        override fun invoke() =
            storage.state.map { state ->
                MicroblogFeedUi(
                    entries = state.entries,
                    sort = state.sort,
                    isInitialLoading = !state.loaded && state.generalResource.isLoading,
                    hasMore = state.hasMore,
                    isLoadingMore = state.isLoadingMore,
                    loadMoreAction = pager::loadMore,
                    selectSortAction = { sort -> reloadWith(sort) },
                    swipeRefresh =
                        SwipeRefreshUi(
                            isRefreshing = state.loaded && state.generalResource.isLoading,
                            refreshAction = { reloadWith(state.sort) },
                        ),
                    errorDialog =
                        state.generalResource.failedAction?.let { failure ->
                            ErrorDialogUi(
                                error = failure.cause,
                                retryAction = failure.retryAction,
                                dismissAction = { storage.update { it.copy(generalResource = Resource.idle()) } },
                            )
                        },
                )
            }

        private fun reloadWith(sort: MicroblogFeedSort) {
            appScopes.safeKeyed<MicroblogFeedScope>(key) {
                withResource(
                    refresh = { pager.reload(sort) },
                    update = { resource -> storage.update { it.copy(generalResource = resource) } },
                    launch = { callback -> appScopes.safeKeyed<MicroblogFeedScope>(key, block = callback) },
                )
            }
        }
    }

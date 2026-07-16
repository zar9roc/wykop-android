package io.github.wykopmobilny.domain.entrydetails

import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsKey
import io.github.wykopmobilny.domain.entrydetails.di.EntryDetailsScope
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.domain.utils.withResource
import io.github.wykopmobilny.entries.details.EntryDetailsUi
import io.github.wykopmobilny.entries.details.GetEntryDetails
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.ui.base.Resource
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.base.components.SwipeRefreshUi
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class GetEntryDetailsQuery
    @Inject
    constructor(
        private val key: EntryDetailsKey,
        private val storage: EntryDetailsStateStorage,
        private val pager: EntryCommentsPager,
        private val appScopes: AppScopes,
    ) : GetEntryDetails {
        override fun invoke() =
            storage.state.map { state ->
                EntryDetailsUi(
                    entry = state.entry,
                    comments = state.comments,
                    totalCommentsCount = state.totalCount,
                    isInitialLoading = state.entry == null && state.generalResource.isLoading,
                    hasOlder = state.hasOlder,
                    hasNewer = state.hasNewer,
                    isLoadingOlder = state.isLoadingOlder,
                    isLoadingNewer = state.isLoadingNewer,
                    loadOlderAction = pager::loadOlder,
                    loadNewerAction = pager::loadNewer,
                    jumpToNewestAction =
                        ((state.lastPage ?: 0) > 1).takeIf { it }?.let {
                            safeCallback { pager.jumpToNewest() }
                        },
                    swipeRefresh =
                        SwipeRefreshUi(
                            isRefreshing = state.entry != null && state.generalResource.isLoading,
                            refreshAction = safeCallback { refresh() },
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

        private suspend fun refresh() {
            withResource(
                refresh = { pager.initialLoad() },
                update = { resource -> storage.update { it.copy(generalResource = resource) } },
                launch = { callback -> appScopes.safeKeyed<EntryDetailsScope>(key, block = callback) },
            )
        }

        private fun safeCallback(function: suspend () -> Unit): () -> Unit =
            {
                appScopes.safeKeyed<EntryDetailsScope>(key) { function() }
            }
    }

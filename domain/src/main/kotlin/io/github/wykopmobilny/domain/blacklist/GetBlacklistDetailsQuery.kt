package io.github.wykopmobilny.domain.blacklist

import io.github.wykopmobilny.domain.blacklist.di.BlacklistScope
import io.github.wykopmobilny.domain.repositories.BlacklistRepository
import io.github.wykopmobilny.domain.repositories.ProfilesRepository
import io.github.wykopmobilny.domain.repositories.TagsRepository
import io.github.wykopmobilny.domain.utils.safe
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.ui.base.FailedAction
import io.github.wykopmobilny.ui.base.ItemState
import io.github.wykopmobilny.ui.base.Resource
import io.github.wykopmobilny.ui.base.components.ErrorDialogUi
import io.github.wykopmobilny.ui.blacklist.BlacklistedDetailsUi
import io.github.wykopmobilny.ui.blacklist.BlacklistedElementUi
import io.github.wykopmobilny.ui.blacklist.GetBlacklistDetails
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@BlacklistScope
internal class GetBlacklistDetailsQuery
    @Inject
    constructor(
        private val viewState: BlacklistViewStateStorage,
        private val blacklistRepository: BlacklistRepository,
        private val tagsRepository: TagsRepository,
        private val profilesRepository: ProfilesRepository,
        private val appScopes: AppScopes,
    ) : GetBlacklistDetails {
        override fun invoke() =
            viewState.state
                .map { state ->
                    BlacklistedDetailsUi(
                        errorDialog =
                            state.generalResource.failedAction?.let { error ->
                                ErrorDialogUi(
                                    error = error.cause,
                                    retryAction = error.retryAction,
                                    dismissAction = {
                                        appScopes.safe<BlacklistScope> {
                                            viewState.update { it.copy(generalResource = Resource.idle()) }
                                        }
                                    },
                                )
                            },
                        users =
                            page(
                                pageState = state.users,
                                unblock = ::unblockUser,
                                submit = ::addUser,
                                suggestions = blacklistRepository::suggestUsers,
                            ),
                        domains =
                            page(
                                pageState = state.domains,
                                unblock = ::unblockDomain,
                                submit = ::addDomain,
                                suggestions = null,
                            ),
                        tags =
                            page(
                                pageState = state.tags,
                                unblock = ::unblockTag,
                                submit = ::addTag,
                                suggestions = blacklistRepository::suggestTags,
                            ),
                    )
                }.onStart { if (viewState.markLoadStartedIfNeeded()) loadAll() }

        private fun page(
            pageState: BlacklistViewState.PageState,
            unblock: (String) -> Unit,
            submit: (String) -> Unit,
            suggestions: (suspend (String) -> List<String>)?,
        ) = BlacklistedDetailsUi.ElementPage(
            isLoading = pageState.loading,
            elements =
                pageState.items
                    .sorted()
                    .map { name ->
                        BlacklistedElementUi(
                            name = name,
                            state =
                                when (val itemState = pageState.itemStates[name]) {
                                    is ItemState.Error ->
                                        BlacklistedElementUi.StateUi.Error(
                                            showError = { showItemError(itemState.error) { unblock(name) } },
                                        )

                                    ItemState.InProgress -> BlacklistedElementUi.StateUi.InProgress

                                    null -> BlacklistedElementUi.StateUi.Default(unblock = { unblock(name) })
                                },
                        )
                    },
            add =
                BlacklistedDetailsUi.AddUi(
                    inProgress = pageState.addInProgress,
                    suggestions = suggestions,
                    submit = submit,
                ),
            refreshAction = ::refresh,
        )

        private fun showItemError(
            error: Throwable,
            retry: () -> Unit,
        ) = appScopes.safe<BlacklistScope> {
            viewState.update {
                it.copy(generalResource = Resource.error(failedAction = FailedAction(cause = error, retryAction = retry)))
            }
        }

        private fun refresh() = loadAll()

        private fun loadAll() {
            loadUsers()
            loadDomains()
            loadTags()
        }

        private fun loadUsers() =
            appScopes.safe<BlacklistScope> {
                viewState.update { it.copy(users = it.users.copy(loading = true)) }
                runCatching { blacklistRepository.getUsers() }
                    .onSuccess { list -> viewState.update { it.copy(users = it.users.copy(loading = false, items = list)) } }
                    .onFailure { error -> onLoadFailure(error) { viewState.update { it.copy(users = it.users.copy(loading = false)) } } }
            }

        private fun loadTags() =
            appScopes.safe<BlacklistScope> {
                viewState.update { it.copy(tags = it.tags.copy(loading = true)) }
                runCatching { blacklistRepository.getTags() }
                    .onSuccess { list -> viewState.update { it.copy(tags = it.tags.copy(loading = false, items = list)) } }
                    .onFailure { error -> onLoadFailure(error) { viewState.update { it.copy(tags = it.tags.copy(loading = false)) } } }
            }

        private fun loadDomains() =
            appScopes.safe<BlacklistScope> {
                viewState.update { it.copy(domains = it.domains.copy(loading = true)) }
                runCatching { blacklistRepository.getDomains() }
                    .onSuccess { list -> viewState.update { it.copy(domains = it.domains.copy(loading = false, items = list)) } }
                    .onFailure { error -> onLoadFailure(error) { viewState.update { it.copy(domains = it.domains.copy(loading = false)) } } }
            }

        private fun onLoadFailure(
            error: Throwable,
            clearLoading: () -> Unit,
        ) {
            clearLoading()
            viewState.update {
                it.copy(generalResource = Resource.error(failedAction = FailedAction(cause = error, retryAction = ::refresh)))
            }
        }

        private fun addUser(name: String) =
            appScopes.safe<BlacklistScope> {
                val clean = name.trim().removePrefix("@")
                if (clean.isBlank()) return@safe
                viewState.update { it.copy(users = it.users.copy(addInProgress = true)) }
                runCatching { profilesRepository.blockUser(clean) }
                    .onSuccess {
                        viewState.update { it.copy(users = it.users.copy(addInProgress = false)) }
                        loadUsers()
                    }.onFailure { error -> onAddFailure(error) { viewState.update { it.copy(users = it.users.copy(addInProgress = false)) } } }
            }

        private fun addTag(name: String) =
            appScopes.safe<BlacklistScope> {
                val clean = name.trim().removePrefix("#")
                if (clean.isBlank()) return@safe
                viewState.update { it.copy(tags = it.tags.copy(addInProgress = true)) }
                runCatching { tagsRepository.blockTag(clean) }
                    .onSuccess {
                        viewState.update { it.copy(tags = it.tags.copy(addInProgress = false)) }
                        loadTags()
                    }.onFailure { error -> onAddFailure(error) { viewState.update { it.copy(tags = it.tags.copy(addInProgress = false)) } } }
            }

        private fun addDomain(name: String) =
            appScopes.safe<BlacklistScope> {
                val clean = name.trim()
                if (clean.isBlank()) return@safe
                viewState.update { it.copy(domains = it.domains.copy(addInProgress = true)) }
                runCatching { blacklistRepository.blockDomain(clean) }
                    .onSuccess {
                        viewState.update { it.copy(domains = it.domains.copy(addInProgress = false)) }
                        loadDomains()
                    }.onFailure { error -> onAddFailure(error) { viewState.update { it.copy(domains = it.domains.copy(addInProgress = false)) } } }
            }

        private fun onAddFailure(
            error: Throwable,
            clearProgress: () -> Unit,
        ) {
            clearProgress()
            viewState.update {
                it.copy(generalResource = Resource.error(failedAction = FailedAction(cause = error, retryAction = {})))
            }
        }

        private fun unblockUser(user: String) =
            appScopes.safe<BlacklistScope> {
                viewState.update { it.copy(users = it.users.copy(itemStates = it.users.itemStates + (user to ItemState.InProgress))) }
                runCatching { profilesRepository.unblockUser(user) }
                    .onSuccess {
                        viewState.update {
                            it.copy(users = it.users.copy(items = it.users.items - user, itemStates = it.users.itemStates - user))
                        }
                    }.onFailure { error ->
                        viewState.update { it.copy(users = it.users.copy(itemStates = it.users.itemStates + (user to ItemState.Error(error)))) }
                    }
            }

        private fun unblockTag(tag: String) =
            appScopes.safe<BlacklistScope> {
                viewState.update { it.copy(tags = it.tags.copy(itemStates = it.tags.itemStates + (tag to ItemState.InProgress))) }
                runCatching { tagsRepository.unblockTag(tag) }
                    .onSuccess {
                        viewState.update {
                            it.copy(tags = it.tags.copy(items = it.tags.items - tag, itemStates = it.tags.itemStates - tag))
                        }
                    }.onFailure { error ->
                        viewState.update { it.copy(tags = it.tags.copy(itemStates = it.tags.itemStates + (tag to ItemState.Error(error)))) }
                    }
            }

        private fun unblockDomain(domain: String) =
            appScopes.safe<BlacklistScope> {
                viewState.update { it.copy(domains = it.domains.copy(itemStates = it.domains.itemStates + (domain to ItemState.InProgress))) }
                runCatching { blacklistRepository.unblockDomain(domain) }
                    .onSuccess {
                        viewState.update {
                            it.copy(domains = it.domains.copy(items = it.domains.items - domain, itemStates = it.domains.itemStates - domain))
                        }
                    }.onFailure { error ->
                        viewState.update {
                            it.copy(domains = it.domains.copy(itemStates = it.domains.itemStates + (domain to ItemState.Error(error))))
                        }
                    }
            }
    }

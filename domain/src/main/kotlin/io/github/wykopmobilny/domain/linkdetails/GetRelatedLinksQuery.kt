package io.github.wykopmobilny.domain.linkdetails

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import io.github.wykopmobilny.data.cache.api.UserVote
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope
import io.github.wykopmobilny.domain.navigation.InteropRequest
import io.github.wykopmobilny.domain.navigation.InteropRequestsProvider
import io.github.wykopmobilny.domain.profile.toUi
import io.github.wykopmobilny.domain.repositories.LinksRepository
import io.github.wykopmobilny.domain.utils.safe
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.links.details.AddRelatedLink
import io.github.wykopmobilny.links.details.GetRelatedLinks
import io.github.wykopmobilny.links.details.RefreshRelatedLinks
import io.github.wykopmobilny.links.details.RelatedLinkUi
import io.github.wykopmobilny.ui.components.widgets.ColorConst
import io.github.wykopmobilny.ui.components.widgets.TwoActionsCounterUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.URL
import javax.inject.Inject

@LinkDetailsScope
internal class GetRelatedLinksQuery
    @Inject
    constructor(
        private val key: LinkDetailsKey,
        private val relatedLinksStore: Store<Long, List<RelatedLink>>,
        private val linksRepository: LinksRepository,
        private val interopRequests: InteropRequestsProvider,
        private val appScopes: AppScopes,
    ) : GetRelatedLinks,
        RefreshRelatedLinks,
        AddRelatedLink {
        override fun invoke(): Flow<List<RelatedLinkUi>> =
            relatedLinksStore
                .stream(StoreReadRequest.cached(key = key.linkId, refresh = false))
                .map { response ->
                    response.dataOrNull().orEmpty().map { it.toUi(linkId = key.linkId) }
                }

        private fun RelatedLink.toUi(linkId: Long) =
            RelatedLinkUi(
                author = author?.toUi(onClicked = null),
                upvotesCount =
                    TwoActionsCounterUi(
                        count = voteCount,
                        color =
                            when (userVote) {
                                UserVote.Up -> ColorConst.CounterUpvoted
                                UserVote.Down -> ColorConst.CounterDownvoted
                                null -> null
                            },
                        upvoteAction =
                            when (userVote) {
                                UserVote.Up -> {
                                    safeCallback {
                                        linksRepository.removeRelatedVote(linkId = linkId, relatedId = id)
                                    }
                                }

                                UserVote.Down, null -> {
                                    safeCallback {
                                        linksRepository.relatedVoteUp(linkId = linkId, relatedId = id)
                                    }
                                }
                            },
                        downvoteAction =
                            when (userVote) {
                                UserVote.Down -> {
                                    safeCallback {
                                        linksRepository.removeRelatedVote(linkId = linkId, relatedId = id)
                                    }
                                }

                                UserVote.Up, null -> {
                                    safeCallback {
                                        linksRepository.relatedVoteDown(linkId = linkId, relatedId = id)
                                    }
                                }
                            },
                    ),
                title = title,
                domain = url.takeIf { it.isNotEmpty() }?.let { URL(it).host.removePrefix("www.") }.orEmpty(),
                clickAction = safeCallback { interopRequests.request(InteropRequest.WebBrowser(url)) },
                shareAction = safeCallback { interopRequests.request(InteropRequest.Share(url)) },
            )

        private fun safeCallback(function: suspend CoroutineScope.() -> Unit): () -> Unit =
            {
                appScopes.safeKeyed<LinkDetailsScope>(id = key) {
                    runCatching { function() }
                }
            }

        override fun refresh() {
            appScopes.safeKeyed<LinkDetailsScope>(id = key) {
                runCatching {
                    relatedLinksStore.fresh(key.linkId)
                }
            }
        }

        override fun add(url: String, title: String) {
            appScopes.safeKeyed<LinkDetailsScope>(id = key) {
                runCatching {
                    linksRepository.addRelated(
                        linkId = key.linkId,
                        url = url,
                        title = title,
                    )
                    relatedLinksStore.fresh(key.linkId)
                }
            }
        }
    }

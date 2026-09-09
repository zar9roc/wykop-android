package io.github.wykopmobilny.domain.linkdetails

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.impl.extensions.fresh
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.errorhandling.KnownError
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope
import io.github.wykopmobilny.domain.profile.LinkInfo
import io.github.wykopmobilny.domain.utils.safeKeyed
import io.github.wykopmobilny.domain.utils.withResource
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.kotlin.AppScopes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import javax.inject.Inject

internal class InitializeLinkDetails
    @Inject
    constructor(
        private val key: LinkDetailsKey,
        private val linkStore: Store<Long, LinkInfo>,
        private val relatedLinksStore: Store<Long, List<RelatedLink>>,
        private val commentsStore: Store<Long, Map<LinkComment, List<LinkComment>>>,
        private val viewStateStorage: LinkDetailsViewStateStorage,
        private val appScopes: AppScopes,
    ) : ScopeInitializer {
        override suspend fun initialize() {
            // Ekran "Powiazane" (osobny scope, source="related") reuzywa danych zapisanych
            // w SoT przez ekran szczegolow - stream w GetRelatedLinksQuery czyta cached(refresh=false).
            // Pobranie sieciowe tylko na jawne odswiezenie (przycisk / pull-to-refresh).
            if (key.source == SOURCE_RELATED) return

            val link =
                withResource(
                    refresh = {
                        try {
                            coroutineScope {
                                launch { commentsStore.fresh(key = key.linkId) }
                                linkStore.fresh(key = key.linkId)
                            }
                        } catch (failure: HttpException) {
                            // Usuniete/nieistniejace znalezisko = 404 - czytelny komunikat
                            // zamiast surowego "HTTP 404".
                            if (failure.code() == HTTP_NOT_FOUND) {
                                throw KnownError.ContentNotFound("To znalezisko zostało usunięte lub nie istnieje.")
                            } else {
                                throw failure
                            }
                        }
                    },
                    update = { resource -> viewStateStorage.update { it.copy(generalResource = resource) } },
                )

            if (link.isSuccess) {
                withResource(
                    refresh = { relatedLinksStore.fresh(key = key.linkId) },
                    update = { resource -> viewStateStorage.update { it.copy(relatedResource = resource) } },
                    launch = { callback -> appScopes.safeKeyed<LinkDetailsScope>(key, block = callback) },
                )
            }
        }

        companion object {
            private const val SOURCE_RELATED = "related"
        }
    }

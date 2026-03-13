package io.github.wykopmobilny.domain.linkdetails.di

import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.domain.api.ApiClient
import io.github.wykopmobilny.domain.di.ScopeInitializer
import io.github.wykopmobilny.domain.linkdetails.GetLinkDetailsQuery
import io.github.wykopmobilny.domain.linkdetails.InitializeLinkDetails
import io.github.wykopmobilny.domain.linkdetails.LinkComment
import io.github.wykopmobilny.domain.linkdetails.RelatedLink
import io.github.wykopmobilny.domain.linkdetails.datasource.linkCommentsSourceOfTruth
import io.github.wykopmobilny.domain.linkdetails.datasource.linkDetailsSourceOfTruth
import io.github.wykopmobilny.domain.linkdetails.datasource.relatedLinksSourceOfTruth
import io.github.wykopmobilny.domain.profile.LinkInfo
import io.github.wykopmobilny.links.details.GetLinkDetails
import io.github.wykopmobilny.kotlin.AppScopes

@Module
internal abstract class LinkDetailsModule {
    companion object {
        @LinkDetailsScope
        @Provides
        fun linkDetailsStore(
            retrofitApi: LinksV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
            apiClient: ApiClient,
        ): Store<Long, LinkInfo> =
            StoreBuilder
                .from(
                    fetcher = apiClient.fetcher(retrofitApi::getLink),
                    sourceOfTruth = linkDetailsSourceOfTruth(cache),
                ).scope(appScopes.applicationScope)
                .build()

        @LinkDetailsScope
        @Provides
        fun linkComments(
            retrofitApi: LinksV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
            apiClient: ApiClient,
        ): Store<Long, Map<LinkComment, List<LinkComment>>> =
            StoreBuilder
                .from(
                    fetcher =
                        apiClient.fetcher { linkId ->
                            retrofitApi.getLinkComments(
                                linkId = linkId,
                                sortBy = "oldest",
                            )
                        },
                    sourceOfTruth = linkCommentsSourceOfTruth(cache),
                ).scope(appScopes.applicationScope)
                .build()

        @LinkDetailsScope
        @Provides
        fun relatedLinksStore(
            retrofitApi: LinksV3RetrofitApi,
            appScopes: AppScopes,
            cache: AppCache,
            apiClient: ApiClient,
        ): Store<Long, List<RelatedLink>> =
            StoreBuilder
                .from(
                    fetcher = apiClient.fetcher(retrofitApi::getRelated),
                    sourceOfTruth = relatedLinksSourceOfTruth(cache),
                ).scope(appScopes.applicationScope)
                .build()
    }

    @Binds
    abstract fun getProfileDetails(impl: GetLinkDetailsQuery): GetLinkDetails

    @Binds
    abstract fun getRelatedLinks(
        impl: io.github.wykopmobilny.domain.linkdetails.GetRelatedLinksQuery,
    ): io.github.wykopmobilny.links.details.GetRelatedLinks

    @Binds
    abstract fun scopeInitializer(impl: InitializeLinkDetails): ScopeInitializer
}

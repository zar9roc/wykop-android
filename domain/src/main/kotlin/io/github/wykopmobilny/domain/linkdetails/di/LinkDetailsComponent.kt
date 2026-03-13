package io.github.wykopmobilny.domain.linkdetails.di

import dagger.BindsInstance
import dagger.Subcomponent
import io.github.wykopmobilny.domain.di.HasScopeInitializer
import io.github.wykopmobilny.links.details.GetLinkDetails

data class LinkDetailsKey(
    val linkId: Long,
    val initialCommentId: Long?,
)

@LinkDetailsScope
@Subcomponent(modules = [LinkDetailsModule::class])
interface LinkDetailsComponent : HasScopeInitializer {
    fun getLinkDetails(): GetLinkDetails

    fun getRelatedLinks(): io.github.wykopmobilny.links.details.GetRelatedLinks

    fun refreshRelatedLinks(): io.github.wykopmobilny.links.details.RefreshRelatedLinks

    fun addRelatedLink(): io.github.wykopmobilny.links.details.AddRelatedLink

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance key: LinkDetailsKey,
        ): LinkDetailsComponent
    }
}

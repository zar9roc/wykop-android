package io.github.wykopmobilny.di.modules

import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.api.addlink.AddLinkApi
import io.github.wykopmobilny.api.addlink.AddLinkRepository
import io.github.wykopmobilny.api.embed.ExternalApi
import io.github.wykopmobilny.api.embed.ExternalRepository
import io.github.wykopmobilny.api.entries.EntriesApi
import io.github.wykopmobilny.api.entries.EntriesRepository
import io.github.wykopmobilny.api.hits.HitsApi
import io.github.wykopmobilny.api.hits.HitsRepository
import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.api.links.LinksRepository
import io.github.wykopmobilny.api.mywykop.MyWykopApi
import io.github.wykopmobilny.api.mywykop.MyWykopRepository
import io.github.wykopmobilny.api.notifications.NotificationsApi
import io.github.wykopmobilny.api.notifications.NotificationsRepository
import io.github.wykopmobilny.api.patrons.PatronsApi
import io.github.wykopmobilny.api.patrons.PatronsRepository
import io.github.wykopmobilny.api.pm.PMApi
import io.github.wykopmobilny.api.pm.PMRepository
import io.github.wykopmobilny.api.profile.ProfileApi
import io.github.wykopmobilny.api.profile.ProfileRepository
import io.github.wykopmobilny.api.search.SearchApi
import io.github.wykopmobilny.api.search.SearchRepository
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.api.suggest.SuggestRepository
import io.github.wykopmobilny.api.tag.TagApi
import io.github.wykopmobilny.api.tag.TagRepository
import io.github.wykopmobilny.api.user.LoginApi
import io.github.wykopmobilny.api.user.LoginRepository

@Module
class RepositoryModule {
    @Provides
    fun provideEntriesApi(impl: EntriesRepository): EntriesApi = impl

    @Provides
    fun notifications(impl: NotificationsRepository): NotificationsApi = impl

    @Provides
    fun myWykop(impl: MyWykopRepository): MyWykopApi = impl

    @Provides
    fun links(impl: LinksRepository): LinksApi = impl

    @Provides
    fun tag(impl: TagRepository): TagApi = impl

    @Provides
    fun login(impl: LoginRepository): LoginApi = impl

    @Provides
    fun pm(impl: PMRepository): PMApi = impl

    @Provides
    fun suggest(impl: SuggestRepository): SuggestApi = impl

    @Provides
    fun search(impl: SearchRepository): SearchApi = impl

    @Provides
    fun hits(impl: HitsRepository): HitsApi = impl

    @Provides
    fun profile(impl: ProfileRepository): ProfileApi = impl

    @Provides
    fun external(impl: ExternalRepository): ExternalApi = impl

    @Provides
    fun addLink(impl: AddLinkRepository): AddLinkApi = impl

    @Provides
    fun patrons(impl: PatronsRepository): PatronsApi = impl
}

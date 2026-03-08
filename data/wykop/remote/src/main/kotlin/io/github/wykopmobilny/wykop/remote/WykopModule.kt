package io.github.wykopmobilny.wykop.remote

import dagger.Module
import dagger.Provides
import dagger.Reusable
import io.github.wykopmobilny.api.endpoints.AddLinkRetrofitApi
import io.github.wykopmobilny.api.endpoints.EntriesRetrofitApi
import io.github.wykopmobilny.api.endpoints.ExternalRetrofitApi
import io.github.wykopmobilny.api.endpoints.HitsRetrofitApi
import io.github.wykopmobilny.api.endpoints.LinksRetrofitApi
import io.github.wykopmobilny.api.endpoints.LoginRetrofitApi
import io.github.wykopmobilny.api.endpoints.MyWykopRetrofitApi
import io.github.wykopmobilny.api.endpoints.NotificationsRetrofitApi
import io.github.wykopmobilny.api.endpoints.PMRetrofitApi
import io.github.wykopmobilny.api.endpoints.SearchRetrofitApi
import io.github.wykopmobilny.api.endpoints.SuggestRetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.AuthV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.HitsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.NotificationsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.TagsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.UsersV3RetrofitApi
import retrofit2.Retrofit
import retrofit2.create

@Module(includes = [RetrofitModule::class])
internal class WykopModule {
    @Reusable
    @Provides
    fun addLinkRetrofitApi(retrofit: Retrofit) = retrofit.create<AddLinkRetrofitApi>()

    @Reusable
    @Provides
    fun entriesRetrofitApi(retrofit: Retrofit) = retrofit.create<EntriesRetrofitApi>()

    @Reusable
    @Provides
    fun externalRetrofitApi(retrofit: Retrofit) = retrofit.create<ExternalRetrofitApi>()

    @Reusable
    @Provides
    fun hitsRetrofitApi(retrofit: Retrofit) = retrofit.create<HitsRetrofitApi>()

    @Reusable
    @Provides
    fun linksRetrofitApi(retrofit: Retrofit) = retrofit.create<LinksRetrofitApi>()

    @Reusable
    @Provides
    fun loginRetrofitApi(retrofit: Retrofit) = retrofit.create<LoginRetrofitApi>()

    @Reusable
    @Provides
    fun myWykopRetrofitApi(retrofit: Retrofit) = retrofit.create<MyWykopRetrofitApi>()

    @Reusable
    @Provides
    fun notificationsRetrofitApi(retrofit: Retrofit) = retrofit.create<NotificationsRetrofitApi>()

    @Reusable
    @Provides
    fun pMRetrofitApi(retrofit: Retrofit) = retrofit.create<PMRetrofitApi>()

    @Reusable
    @Provides
    fun searchRetrofitApi(retrofit: Retrofit) = retrofit.create<SearchRetrofitApi>()

    @Reusable
    @Provides
    fun suggestRetrofitApi(retrofit: Retrofit) = retrofit.create<SuggestRetrofitApi>()

    @Reusable
    @Provides
    fun entriesV3RetrofitApi(retrofit: Retrofit) = retrofit.create<EntriesV3RetrofitApi>()

    @Reusable
    @Provides
    fun linksV3RetrofitApi(retrofit: Retrofit) = retrofit.create<LinksV3RetrofitApi>()

    @Reusable
    @Provides
    fun hitsV3RetrofitApi(retrofit: Retrofit) = retrofit.create<HitsV3RetrofitApi>()

    @Reusable
    @Provides
    fun authV3RetrofitApi(retrofit: Retrofit) = retrofit.create<AuthV3RetrofitApi>()

    @Reusable
    @Provides
    fun usersV3RetrofitApi(retrofit: Retrofit) = retrofit.create<UsersV3RetrofitApi>()

    @Reusable
    @Provides
    fun notificationsV3RetrofitApi(retrofit: Retrofit) = retrofit.create<NotificationsV3RetrofitApi>()

    @Reusable
    @Provides
    fun profileV3RetrofitApi(retrofit: Retrofit) = retrofit.create<ProfileV3RetrofitApi>()

    @Reusable
    @Provides
    fun tagsV3RetrofitApi(retrofit: Retrofit) = retrofit.create<TagsV3RetrofitApi>()

    @Reusable
    @Provides
    fun mediaV3RetrofitApi(retrofit: Retrofit) = retrofit.create<MediaV3RetrofitApi>()
}

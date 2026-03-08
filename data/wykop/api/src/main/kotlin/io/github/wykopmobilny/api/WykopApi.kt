package io.github.wykopmobilny.api

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
import io.github.wykopmobilny.api.endpoints.v3.FavouritesV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.EntriesV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.HitsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.LinksV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.MediaV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.NotificationsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.ObservedV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.TagsV3RetrofitApi
import io.github.wykopmobilny.api.endpoints.v3.UsersV3RetrofitApi

interface WykopApi {
    fun addLinkRetrofitApi(): AddLinkRetrofitApi

    fun entriesRetrofitApi(): EntriesRetrofitApi

    fun externalRetrofitApi(): ExternalRetrofitApi

    fun hitsRetrofitApi(): HitsRetrofitApi

    fun linksRetrofitApi(): LinksRetrofitApi

    fun loginRetrofitApi(): LoginRetrofitApi

    fun myWykopRetrofitApi(): MyWykopRetrofitApi

    fun notificationsRetrofitApi(): NotificationsRetrofitApi

    fun pMRetrofitApi(): PMRetrofitApi

    fun searchRetrofitApi(): SearchRetrofitApi

    fun suggestRetrofitApi(): SuggestRetrofitApi

    fun entriesV3RetrofitApi(): EntriesV3RetrofitApi

    fun linksV3RetrofitApi(): LinksV3RetrofitApi

    fun hitsV3RetrofitApi(): HitsV3RetrofitApi

    fun authV3RetrofitApi(): AuthV3RetrofitApi

    fun usersV3RetrofitApi(): UsersV3RetrofitApi

    fun notificationsV3RetrofitApi(): NotificationsV3RetrofitApi

    fun profileV3RetrofitApi(): ProfileV3RetrofitApi

    fun tagsV3RetrofitApi(): TagsV3RetrofitApi

    fun mediaV3RetrofitApi(): MediaV3RetrofitApi

    fun observedV3RetrofitApi(): ObservedV3RetrofitApi

    fun favouritesV3RetrofitApi(): FavouritesV3RetrofitApi

    fun errorBodyParser(): ErrorBodyParser

    fun errorBodyParserV3(): ErrorBodyParserV3
}

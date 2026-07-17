package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.config.ConfigResponseV3
import retrofit2.http.GET

interface ConfigV3RetrofitApi {
    // Konfiguracja klienta (m.in. kolory rang nicków) - pobierana raz na tydzień.
    @GET("v3/config")
    suspend fun getConfig(): WykopApiResponseV3<ConfigResponseV3>
}

package io.github.wykopmobilny.api.endpoints.v3

import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import io.github.wykopmobilny.api.requests.v3.reports.CreateProfileReportRequestV3
import io.github.wykopmobilny.api.requests.v3.reports.CreateReportRequestV3
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.reports.ReportResponseV3
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportsV3RetrofitApi {
    @POST("v3/reports/reports")
    suspend fun createReport(
        @Body request: WykopApiRequestV3<CreateReportRequestV3>,
    ): WykopApiResponseV3<ReportResponseV3>

    // Ten sam endpoint, ale profil ma `id` stringowe (username) - stad osobne body.
    @POST("v3/reports/reports")
    suspend fun createProfileReport(
        @Body request: WykopApiRequestV3<CreateProfileReportRequestV3>,
    ): WykopApiResponseV3<ReportResponseV3>
}

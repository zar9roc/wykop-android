package io.github.wykopmobilny.api.requests.v3.reports

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.github.wykopmobilny.api.requests.v3.common.WykopApiRequestV3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateReportRequestV3Test {
    private val adapter =
        Moshi
            .Builder()
            .build()
            .adapter<WykopApiRequestV3<CreateReportRequestV3>>(
                Types.newParameterizedType(WykopApiRequestV3::class.java, CreateReportRequestV3::class.java),
            )

    @Test
    fun `tresc bez rodzica nie wysyla parent_id`() {
        assertEquals(
            """{"data":{"type":"entry","id":87643751}}""",
            adapter.toJson(WykopApiRequestV3(CreateReportRequestV3(type = "entry", id = 87643751))),
        )
    }

    @Test
    fun `komentarz wysyla parent_id`() {
        assertEquals(
            """{"data":{"type":"entry_comment","id":299676307,"parent_id":87643751}}""",
            adapter.toJson(
                WykopApiRequestV3(
                    CreateReportRequestV3(type = "entry_comment", id = 299676307, parentId = 87643751),
                ),
            ),
        )
    }
}

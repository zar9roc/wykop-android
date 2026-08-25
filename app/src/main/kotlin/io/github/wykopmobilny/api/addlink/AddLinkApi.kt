package io.github.wykopmobilny.api.addlink

import io.github.wykopmobilny.api.responses.AddLinkPreviewImage
import io.github.wykopmobilny.api.responses.NewLinkResponse
import io.reactivex.Single

interface AddLinkApi {
    fun getDraft(url: String): Single<NewLinkResponse>

    fun getImages(key: String): Single<List<AddLinkPreviewImage>>

    // Publikacja draftu. API v3 zwraca 201 bez tresci (brak Link), stad Single<Unit>.
    fun publishLink(
        key: String,
        title: String,
        description: String,
        tags: String,
        photo: String,
        url: String,
        plus18: Boolean,
    ): Single<Unit>

    // Edycja opublikowanego znaleziska (PUT /v3/links/{linkId}).
    fun editLink(
        linkId: Long,
        title: String,
        description: String,
        tags: String,
        plus18: Boolean,
    ): Single<Unit>
}

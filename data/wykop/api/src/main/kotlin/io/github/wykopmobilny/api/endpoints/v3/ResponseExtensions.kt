package io.github.wykopmobilny.api.endpoints.v3

import retrofit2.HttpException
import retrofit2.Response

// 204 No Content: Retrofit rzuca NPE przy nienullowalnym zwrocie z pustym body
// (ignoruje '?'), a body-owe WykopApiResponseV3 nie parsuje sie z pustej odpowiedzi.
// Response<Unit> pozwala sprawdzic status; 409 = zasob juz w docelowym stanie (np.
// komentarz usuniety rownolegle) - traktujemy jak sukces.
fun Response<Unit>.requireSuccessful() {
    if (!isSuccessful && code() != 409) throw HttpException(this)
}

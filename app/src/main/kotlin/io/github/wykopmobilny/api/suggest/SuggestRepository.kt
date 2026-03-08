package io.github.wykopmobilny.api.suggest

import io.github.wykopmobilny.api.ErrorBodyParserV3
import io.github.wykopmobilny.api.UserTokenRefresher
import io.github.wykopmobilny.api.endpoints.v3.SuggestV3RetrofitApi
import io.github.wykopmobilny.api.errorhandler.ErrorHandlerTransformerV3
import io.github.wykopmobilny.api.responses.v3.suggest.TagSuggestionResponseV3
import io.github.wykopmobilny.api.responses.v3.suggest.UserSuggestionResponseV3
import io.github.wykopmobilny.models.dataclass.Author
import io.github.wykopmobilny.models.dataclass.TagSuggestion
import io.github.wykopmobilny.utils.api.colorNameToGroupId
import kotlinx.coroutines.rx2.rxSingle
import javax.inject.Inject

class SuggestRepository
    @Inject
    constructor(
        private val suggestApi: SuggestV3RetrofitApi,
        private val userTokenRefresher: UserTokenRefresher,
        private val errorBodyParser: ErrorBodyParserV3,
    ) : SuggestApi {
        override fun getTagSuggestions(suggestionString: String) =
            rxSingle { suggestApi.getTagSuggestions(suggestionString) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<TagSuggestionResponseV3>>(errorBodyParser))
                .map { it.map { response -> TagSuggestion(response.name, response.observedQty) } }

        override fun getUserSuggestions(suggestionString: String) =
            rxSingle { suggestApi.getUserSuggestions(suggestionString) }
                .retryWhen(userTokenRefresher)
                .compose(ErrorHandlerTransformerV3<List<UserSuggestionResponseV3>>(errorBodyParser))
                .map {
                    it.map { response ->
                        Author(
                            response.username,
                            response.avatar.orEmpty(),
                            colorNameToGroupId(response.color),
                            response.gender.orEmpty(),
                        )
                    }
                }
    }

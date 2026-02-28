package io.github.wykopmobilny.utils.usermanager

import android.content.Context
import io.github.wykopmobilny.api.endpoints.v3.UsersV3RetrofitApi
import io.github.wykopmobilny.api.responses.LoginResponse
import io.github.wykopmobilny.api.responses.v3.auth.AuthResponseV3
import io.github.wykopmobilny.storage.api.JwtToken
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.storage.api.LoggedUserInfo
import io.github.wykopmobilny.storage.api.SessionStorage
import io.github.wykopmobilny.storage.api.UserInfoStorage
import io.github.wykopmobilny.kotlin.AppDispatchers
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.ui.dialogs.userNotLoggedInDialog
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class UserCredentials(
    val login: String,
    val avatarUrl: String,
    val backgroundUrl: String?,
    val userKey: String,
)

interface SimpleUserManagerApi {
    fun getUserCredentials(): UserCredentials?
}

interface UserManagerApi : SimpleUserManagerApi {
    suspend fun logoutUser()

    suspend fun saveCredentials(credentials: LoginResponse)

    suspend fun saveJwtCredentials(
        username: String,
        authResponse: AuthResponseV3,
    )

    suspend fun getJwtToken(): JwtToken?

    suspend fun isJwtAuthorized(): Boolean

    fun runIfLoggedIn(
        context: Context,
        callback: () -> Unit,
    )
}

fun UserManagerApi.isUserAuthorized() = getUserCredentials() != null

@Singleton
class UserManager
    @Inject
    constructor(
        private val sessionStorage: SessionStorage,
        private val userInfoStorage: UserInfoStorage,
        private val jwtTokenStorage: JwtTokenStorage,
        private val usersV3Api: UsersV3RetrofitApi,
        private val appScopes: AppScopes,
    ) : UserManagerApi {
        private val userInfo =
            userInfoStorage.loggedUser
                .stateIn(appScopes.applicationScope, SharingStarted.Eagerly, null)

        override suspend fun logoutUser() {
            sessionStorage.updateSession(null)
            userInfoStorage.updateLoggedUser(null)
            jwtTokenStorage.updateJwtToken(null)
            userInfo.first { it == null }
        }

        override suspend fun saveCredentials(credentials: LoginResponse) {
            userInfoStorage.updateLoggedUser(
                value =
                    LoggedUserInfo(
                        id = credentials.profile.id,
                        userToken = credentials.userkey,
                        avatarUrl = credentials.profile.avatar,
                        backgroundUrl = credentials.profile.background,
                    ),
            )
        }

        override suspend fun saveJwtCredentials(
            username: String,
            authResponse: AuthResponseV3,
        ) {
            val expiresAt = System.currentTimeMillis() + (authResponse.expiresIn * 1000)
            jwtTokenStorage.updateJwtToken(
                JwtToken(
                    accessToken = authResponse.token,
                    refreshToken = authResponse.refreshToken,
                    expiresAt = expiresAt,
                ),
            )

            // Fetch user profile using JWT
            val profileResponse = usersV3Api.getUserProfile()
            profileResponse.data?.let { profile ->
                userInfoStorage.updateLoggedUser(
                    LoggedUserInfo(
                        id = profile.username,
                        userToken = "", // Empty for JWT flow (legacy field)
                        avatarUrl = profile.avatar,
                        backgroundUrl = profile.background,
                    ),
                )
            }
        }

        override suspend fun getJwtToken(): JwtToken? = jwtTokenStorage.jwtToken.first()

        override suspend fun isJwtAuthorized(): Boolean = jwtTokenStorage.jwtToken.first() != null

        override fun getUserCredentials(): UserCredentials? =
            userInfo.value?.let {
                UserCredentials(
                    login = it.id,
                    avatarUrl = it.avatarUrl,
                    backgroundUrl = it.backgroundUrl,
                    userKey = it.userToken,
                )
            }

        override fun runIfLoggedIn(
            context: Context,
            callback: () -> Unit,
        ) {
            appScopes.applicationScope.launch(Dispatchers.Main, start = CoroutineStart.UNDISPATCHED) {
                val isLoggedIn = sessionStorage.session.first()
                if (isLoggedIn != null) {
                    callback.invoke()
                } else {
                    withContext(AppDispatchers.Main) {
                        userNotLoggedInDialog(context)?.show()
                    }
                }
            }
        }
    }

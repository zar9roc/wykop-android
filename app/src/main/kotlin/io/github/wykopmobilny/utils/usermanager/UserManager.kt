package io.github.wykopmobilny.utils.usermanager

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import io.github.wykopmobilny.api.responses.LoginResponse
import io.github.wykopmobilny.storage.api.JwtToken
import io.github.wykopmobilny.storage.api.JwtTokenStorage
import io.github.wykopmobilny.storage.api.LoggedUserInfo
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
        private val userInfoStorage: UserInfoStorage,
        private val jwtTokenStorage: JwtTokenStorage,
        private val appScopes: AppScopes,
    ) : UserManagerApi {
        private val userInfo =
            userInfoStorage.loggedUser
                .stateIn(appScopes.applicationScope, SharingStarted.Eagerly, null)

        override suspend fun logoutUser() {
            userInfoStorage.updateLoggedUser(null)
            jwtTokenStorage.updateJwtToken(null)
            clearWebViewSession()
            userInfo.first { it == null }
        }

        // Sesja wykop.pl zyje tez w cookies/localStorage WebView z ekranu logowania -
        // bez wyczyszczenia strona connect loguje ponownie bez pytania o haslo.
        private suspend fun clearWebViewSession() =
            withContext(Dispatchers.Main) {
                suspendCoroutine { continuation ->
                    CookieManager.getInstance().removeAllCookies {
                        CookieManager.getInstance().flush()
                        continuation.resume(Unit)
                    }
                }
                WebStorage.getInstance().deleteAllData()
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
                val isLoggedIn = jwtTokenStorage.jwtToken.first()
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

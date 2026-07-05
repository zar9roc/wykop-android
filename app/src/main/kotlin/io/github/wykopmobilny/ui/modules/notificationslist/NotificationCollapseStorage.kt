package io.github.wykopmobilny.ui.modules.notificationslist

import android.content.Context
import io.github.wykopmobilny.WykopApp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trwały (SharedPreferences) stan zwinięcia nagłówków-akordeonów powiadomień.
 * Klucz = `Notification.tag` nagłówka (nazwa taga w zakładce "Obserwowane tagi",
 * URL celu w zakładce "Do mnie" — nie kolidują). Przeżywa odświeżenie listy,
 * ponowne wejście na ekran i restart aplikacji.
 */
@Singleton
class NotificationCollapseStorage
    @Inject
    constructor(
        app: WykopApp,
    ) {
        private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        private fun collapsedKeys(): MutableSet<String> =
            prefs.getStringSet(KEY_COLLAPSED, emptySet()).orEmpty().toMutableSet()

        fun isCollapsed(tag: String): Boolean = tag in collapsedKeys()

        fun setCollapsed(
            tag: String,
            collapsed: Boolean,
        ) {
            val keys = collapsedKeys()
            val changed = if (collapsed) keys.add(tag) else keys.remove(tag)
            if (changed) {
                prefs.edit().putStringSet(KEY_COLLAPSED, keys).apply()
            }
        }

        private companion object {
            const val PREFS_NAME = "notification_collapse"
            const val KEY_COLLAPSED = "collapsed_tags"
        }
    }

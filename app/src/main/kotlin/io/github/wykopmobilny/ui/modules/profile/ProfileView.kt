package io.github.wykopmobilny.ui.modules.profile

import io.github.wykopmobilny.api.responses.ObserveStateResponse
import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserFullResponseV3
import io.github.wykopmobilny.base.BaseView

interface ProfileView : BaseView {
    fun showProfile(profileResponse: UserFullResponseV3)

    fun showButtons(observeState: ObserveStateResponse)

    fun showBadges(badges: List<BadgeResponseV3>)
}

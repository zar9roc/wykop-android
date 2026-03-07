package io.github.wykopmobilny.ui.modules.profile.badge

import io.github.wykopmobilny.api.responses.v3.profile.BadgeResponseV3
import io.github.wykopmobilny.base.BaseView

interface BadgeView : BaseView {
    fun addDataToAdapter(
        entryList: List<BadgeResponseV3>,
        shouldClearAdapter: Boolean,
    )

    fun disableLoading()
}

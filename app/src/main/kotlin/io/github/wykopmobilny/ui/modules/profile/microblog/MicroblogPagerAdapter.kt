package io.github.wykopmobilny.ui.modules.profile.microblog

import android.content.res.Resources
import android.util.SparseArray
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.github.wykopmobilny.R
import io.github.wykopmobilny.ui.modules.profile.microblog.comments.MicroblogCommentsFragment
import io.github.wykopmobilny.ui.modules.profile.microblog.entries.MicroblogEntriesFragment
import io.github.wykopmobilny.ui.modules.profile.microblog.voted.MicroblogVotedEntriesFragment

class MicroblogPagerAdapter(
    private val resources: Resources,
    fragmentManager: FragmentManager,
) : androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {
    val registeredFragments = SparseArray<Fragment>()

    override fun getItem(position: Int): Fragment =
        when (position) {
            0 -> MicroblogEntriesFragment.newInstance()
            1 -> MicroblogCommentsFragment.newInstance()
            else -> MicroblogVotedEntriesFragment.newInstance()
        }

    override fun getCount() = 3

    override fun instantiateItem(
        container: ViewGroup,
        position: Int,
    ): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any,
    ) {
        registeredFragments.removeAt(position)
        super.destroyItem(container, position, `object`)
    }

    override fun getPageTitle(position: Int) =
        when (position) {
            0 -> R.string.entries
            1 -> R.string.commented
            else -> R.string.entries_voted
        }.let(resources::getString)
}

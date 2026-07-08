package io.github.wykopmobilny.ui.blacklist.android

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.wykopmobilny.ui.blacklist.android.page.BlacklistPageFragment
import io.github.wykopmobilny.ui.blacklist.android.page.BlacklistPageType

internal class BlacklistAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment.childFragmentManager, fragment.viewLifecycleOwner.lifecycle) {
    override fun getItemCount() = 3

    fun getTitle(position: Int) =
        when (position) {
            0 -> R.string.blacklist_tab_title_users
            1 -> R.string.blacklist_tab_title_domains
            2 -> R.string.blacklist_tab_title_tags
            else -> error("unsupported")
        }

    override fun createFragment(position: Int) =
        when (position) {
            0 -> BlacklistPageFragment.newInstance(BlacklistPageType.USERS)
            1 -> BlacklistPageFragment.newInstance(BlacklistPageType.DOMAINS)
            2 -> BlacklistPageFragment.newInstance(BlacklistPageType.TAGS)
            else -> error("unsupported")
        }
}

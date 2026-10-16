package io.github.wykopmobilny.ui.modules.profile.links

import io.github.wykopmobilny.R
import io.github.wykopmobilny.base.BaseFragment
import io.github.wykopmobilny.databinding.ProfileSubtabLayoutBinding
import io.github.wykopmobilny.ui.modules.profile.ProfileActivity
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.github.wykopmobilny.utils.viewBinding
import javax.inject.Inject

class LinksFragment : BaseFragment(R.layout.profile_subtab_layout) {
    companion object {
        fun newInstance() = LinksFragment()
    }

    @Inject
    lateinit var userManagerApi: UserManagerApi

    private val pagerAdapter by lazy {
        val isOwnProfile = userManagerApi.getUserCredentials()?.login == (activity as ProfileActivity).username
        LinksPagerAdapter(resources, childFragmentManager, showBuried = isOwnProfile)
    }

    private val binding by viewBinding(ProfileSubtabLayoutBinding::bind)

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        if (isVisibleToUser && binding.pager.adapter != pagerAdapter) {
            binding.pager.offscreenPageLimit = 1
            binding.pager.adapter = pagerAdapter
            binding.tabLayout.setupWithViewPager(binding.pager)
        }
    }
}

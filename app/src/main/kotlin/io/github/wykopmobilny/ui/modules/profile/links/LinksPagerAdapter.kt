package io.github.wykopmobilny.ui.modules.profile.links

import android.content.res.Resources
import android.util.SparseArray
import android.view.ViewGroup
import io.github.wykopmobilny.R
import io.github.wykopmobilny.ui.modules.profile.actions.ActionsFragment
import io.github.wykopmobilny.ui.modules.profile.actions.ActionsFragmentPresenter
import io.github.wykopmobilny.ui.modules.profile.links.added.ProfileLinksFragment

class LinksPagerAdapter(
    val resources: Resources,
    fragmentManager: androidx.fragment.app.FragmentManager,
    // "Zakopywane" sa dostepne tylko dla wlasnego profilu - dla innych API zwraca 400.
    private val showBuried: Boolean,
) : androidx.fragment.app.FragmentPagerAdapter(fragmentManager) {
    val registeredFragments = SparseArray<androidx.fragment.app.Fragment>()

    override fun getItem(position: Int): androidx.fragment.app.Fragment =
        when (position) {
            0 -> ProfileLinksFragment.newInstance(ProfileLinksFragment.TYPE_ADDED)
            1 -> ProfileLinksFragment.newInstance(ProfileLinksFragment.TYPE_PUBLISHED)
            2 -> ActionsFragment.newInstance(ActionsFragmentPresenter.Source.LINK_COMMENTS)
            3 -> ActionsFragment.newInstance(ActionsFragmentPresenter.Source.LINK_RELATED)
            4 -> ProfileLinksFragment.newInstance(ProfileLinksFragment.TYPE_DIGGED)
            else -> ProfileLinksFragment.newInstance(ProfileLinksFragment.TYPE_BURRIED)
        }

    override fun getCount() = if (showBuried) 6 else 5

    override fun instantiateItem(
        container: ViewGroup,
        position: Int,
    ): Any {
        val fragment = super.instantiateItem(container, position) as androidx.fragment.app.Fragment
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

    override fun getPageTitle(position: Int): CharSequence {
        super.getPageTitle(position)
        return when (position) {
            0 -> resources.getString(R.string.link_added)
            1 -> resources.getString(R.string.published)
            2 -> resources.getString(R.string.commented)
            3 -> resources.getString(R.string.related)
            4 -> resources.getString(R.string.digged)
            else -> resources.getString(R.string.buried)
        }
    }
}

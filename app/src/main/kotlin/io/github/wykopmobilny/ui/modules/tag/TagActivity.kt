package io.github.wykopmobilny.ui.modules.tag

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.MenuCompat
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.responses.ObserveStateResponse
import io.github.wykopmobilny.api.responses.TagMetaResponse
import io.github.wykopmobilny.base.BaseActivity
import io.github.wykopmobilny.data.storage.api.AppStorage
import io.github.wykopmobilny.data.storage.api.PreferenceEntity
import io.github.wykopmobilny.databinding.ActivityTagBinding
import io.github.wykopmobilny.ui.dialogs.MonthYearPickerDialog
import io.github.wykopmobilny.ui.modules.NavigatorApi
import io.github.wykopmobilny.utils.loadImage
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import io.github.wykopmobilny.utils.viewBinding
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import io.github.wykopmobilny.ui.base.android.R as BaseR

class TagActivity :
    BaseActivity(),
    TagActivityView {
    companion object {
        private const val EXTRA_TAG = "EXTRA_TAG"

        // Wartosci sortowania strumienia tagu wg API v3 (/v3/tags/{tag}/stream):
        // "all" = najnowsze (chronologicznie), "best" = najlepsze (wg oceny).
        const val SORT_NEWEST = "all"
        const val SORT_BEST = "best"

        // Zapamietana ostatnio otwarta zakladka (0 = Znaleziska, 1 = Wpisy).
        private const val PREF_LAST_TAB = "tag.last_tab"

        fun createIntent(
            context: Context,
            tag: String,
        ): Intent {
            val intent = Intent(context, TagActivity::class.java)
            intent.putExtra(EXTRA_TAG, tag)
            return intent
        }
    }

    @Inject
    lateinit var navigator: NavigatorApi

    @Inject
    lateinit var presenter: TagActivityPresenter

    @Inject
    lateinit var userManagerApi: UserManagerApi

    @Inject
    lateinit var appStorage: AppStorage

    private val binding by viewBinding(ActivityTagBinding::inflate)

    override val enableSwipeBackLayout: Boolean = true
    private var tagMeta: TagMetaResponse? = null

    // Wspolny filtr dla obu zakladek (Znaleziska/Wpisy). Menu nalezy do aktywnosci,
    // wiec to ona trzyma stan i rozsyla go do zakladek.
    var tagSort: String = SORT_NEWEST
        private set
    var tagArchiveYear: Int? = null
        private set
    var tagArchiveMonth: Int? = null
        private set
    private val isArchival get() = tagArchiveYear != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar.toolbar)
        presenter.subscribe(this)
        supportFragmentManager.setFragmentResultListener(MonthYearPickerDialog.REQUEST_KEY, this) { _, bundle ->
            val year = bundle.getInt(MonthYearPickerDialog.EXTRA_YEAR)
            val month = if (bundle.containsKey(MonthYearPickerDialog.EXTRA_MONTH)) {
                bundle.getInt(MonthYearPickerDialog.EXTRA_MONTH)
            } else {
                null
            }
            setArchive(year, month)
        }
        val tagString =
            intent.getStringExtra(EXTRA_TAG) ?: return finish().also {
                Napier.e(message = "Couldn't launch TagActivity ${savedInstanceState == null}")
            }
        presenter.tag = tagString
        binding.fab.setOnClickListener {
            navigator.openAddEntryActivity(this, null, "#$tagString")
        }
        binding.fab.isVisible = userManagerApi.isUserAuthorized()

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "\n#$tagString"
        }

        val adapter = TagPagerAdapter(tagString, resources, supportFragmentManager)
        binding.pager.adapter = adapter
        tagMeta?.let {
            setMeta(tagMeta!!)
        }
        binding.tabLayout.setupWithViewPager(binding.pager)
        restoreLastTab()
        presenter.loadTagDetails()
    }

    // Przywraca ostatnio otwarta zakladke i zapisuje kazda zmiane, by przy kolejnym
    // wejsciu w tag od razu pokazac Znaleziska/Wpisy zgodnie z ostatnim wyborem.
    private fun restoreLastTab() {
        val savedTab = runBlocking {
            appStorage.preferencesQueries.getPreference(PREF_LAST_TAB).executeAsOneOrNull()
        }?.toIntOrNull()?.coerceIn(0, 1) ?: 0
        binding.pager.setCurrentItem(savedTab, false)
        // Listener dodany po ustawieniu pozycji - programowe setCurrentItem go nie odpali.
        binding.pager.addOnPageChangeListener(
            object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    appStorage.preferencesQueries.insertOrReplace(
                        preferenceEntity = PreferenceEntity(key = PREF_LAST_TAB, position.toString()),
                    )
                }
            },
        )
    }

    override fun onDestroy() {
        presenter.unsubscribe()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tag_menu, menu)
        // Rysuje separatory miedzy sekcjami menu (grupy: akcje / sortowanie / archiwum).
        MenuCompat.setGroupDividerEnabled(menu, true)
        if (userManagerApi.isUserAuthorized()) {
            tagMeta?.apply {
                menu.apply {
                    if (isObserved) {
                        findItem(R.id.action_unobserve).isVisible = true
                    } else if (!isBlocked) {
                        findItem(R.id.action_observe).isVisible = true
                        findItem(R.id.action_block).isVisible = true
                    } else if (isBlocked) {
                        findItem(R.id.action_unblock).isVisible = true
                    }
                }
            }
        }
        val sortItemId = if (tagSort == SORT_BEST) R.id.tag_sort_best else R.id.tag_sort_newest
        menu.findItem(sortItemId).isChecked = true
        menu.findItem(R.id.tag_archive_reset).isVisible = isArchival
        menu.findItem(R.id.tag_archive).title = archiveMenuTitle()
        return true
    }

    // "Archiwum" a w trybie archiwalnym "Archiwum (mm/RRRR)" (lub "(RRRR)" dla calego roku).
    private fun archiveMenuTitle(): String {
        val base = getString(R.string.tag_archive)
        val year = tagArchiveYear ?: return base
        val period = tagArchiveMonth?.let { month -> "%02d/%d".format(month, year) } ?: year.toString()
        return "$base ($period)"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_observe -> {
                presenter.observeTag()
            }

            R.id.action_unobserve -> {
                presenter.unobserveTag()
            }

            R.id.action_block -> {
                presenter.blockTag()
            }

            R.id.action_unblock -> {
                presenter.unblockTag()
            }

            android.R.id.home -> {
                finish()
            }

            R.id.refresh -> {
                val tagPagerAdapter = binding.pager.adapter as TagPagerAdapter
                for (i in 0 until tagPagerAdapter.registeredFragments.size()) {
                    (tagPagerAdapter.registeredFragments.valueAt(i) as? SwipeRefreshLayout.OnRefreshListener)?.onRefresh()
                }
            }

            R.id.tag_sort_newest -> setSort(SORT_NEWEST)

            R.id.tag_sort_best -> setSort(SORT_BEST)

            R.id.tag_archive -> {
                // Wstepnie ustawiamy wskazniki popupu na aktualny filtr: konkretny
                // miesiac, "Caly rok" (WHOLE_YEAR) albo domyslnie biezaca data (brak filtra).
                val presetMonth = when {
                    tagArchiveYear == null -> 0
                    tagArchiveMonth == null -> MonthYearPickerDialog.WHOLE_YEAR
                    else -> tagArchiveMonth!!
                }
                MonthYearPickerDialog
                    .newInstance(
                        selectedMonth = presetMonth,
                        selectedYear = tagArchiveYear ?: 0,
                    ).show(supportFragmentManager, "pickerDialogFragment")
            }

            R.id.tag_archive_reset -> resetArchive()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setSort(sort: String) {
        if (tagSort == sort) return
        tagSort = sort
        invalidateOptionsMenu()
        applyFilterToAllTabs()
    }

    private fun setArchive(
        year: Int,
        month: Int?,
    ) {
        tagArchiveYear = year
        tagArchiveMonth = month
        invalidateOptionsMenu()
        applyFilterToAllTabs()
    }

    private fun resetArchive() {
        if (!isArchival) return
        tagArchiveYear = null
        tagArchiveMonth = null
        invalidateOptionsMenu()
        applyFilterToAllTabs()
    }

    // Filtr jest wspolny - stosujemy go do wszystkich utworzonych zakladek naraz.
    private fun applyFilterToAllTabs() {
        val tagPagerAdapter = binding.pager.adapter as? TagPagerAdapter ?: return
        for (i in 0 until tagPagerAdapter.registeredFragments.size()) {
            (tagPagerAdapter.registeredFragments.valueAt(i) as? TagFilterableFragment)
                ?.applyTagFilter(tagSort, tagArchiveYear, tagArchiveMonth)
        }
    }

    override fun setMeta(tagMeta: TagMetaResponse) {
        this.tagMeta = tagMeta
        binding.backgroundImg.isVisible = tagMeta.background != null
        tagMeta.background?.let { background ->
            binding.backgroundImg.loadImage(background)
            binding.toolbar.toolbar.setBackgroundResource(BaseR.drawable.gradient_toolbar_up)
        }
        invalidateOptionsMenu()
    }

    override fun setObserveState(tagState: ObserveStateResponse) {
        tagMeta?.isBlocked = tagState.isBlocked
        tagMeta?.isObserved = tagState.isObserved
        invalidateOptionsMenu()
    }
}

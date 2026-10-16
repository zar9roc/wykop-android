package io.github.wykopmobilny.ui.modules.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.api.endpoints.v3.ProfileV3RetrofitApi
import io.github.wykopmobilny.api.responses.v3.common.WykopApiResponseV3
import io.github.wykopmobilny.api.responses.v3.tags.ShortTagResponseV3
import io.github.wykopmobilny.api.responses.v3.user.UserShortResponseV3
import io.github.wykopmobilny.base.BaseActivity
import io.github.wykopmobilny.databinding.ActivityProfileObservationsBinding
import io.github.wykopmobilny.databinding.ConversationListItemBinding
import io.github.wykopmobilny.databinding.ProfileObservedTagItemBinding
import io.github.wykopmobilny.databinding.ProfileObservationsPageBinding
import io.github.wykopmobilny.models.mapper.apiv3.AuthorMapperV3
import io.github.wykopmobilny.ui.modules.tag.TagActivity
import io.github.wykopmobilny.utils.api.getGroupColor
import io.github.wykopmobilny.utils.viewBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import javax.inject.Inject

/**
 * Obserwujacy / obserwowani uzytkownika (otwierane z naglowka profilu).
 * Zakladki: "Obserwujacy" oraz "Obserwowane" z drugim rzedem "Uzytkownicy" / "Tagi".
 * Kazda lista ma wlasny stan i paginacje (infinite scroll), ladowana leniwie przy
 * pierwszym pokazaniu.
 */
class ProfileObservationsActivity : BaseActivity() {
    @Inject
    lateinit var profileApi: ProfileV3RetrofitApi

    private val binding by viewBinding(ActivityProfileObservationsBinding::inflate)
    private val username by lazy { intent.getStringExtra(EXTRA_USERNAME)!! }

    private lateinit var followersPage: ObservationsPage<UserShortResponseV3>
    private lateinit var followingUsersPage: ObservationsPage<UserShortResponseV3>
    private lateinit var followingTagsPage: ObservationsPage<ShortTagResponseV3>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolbar = binding.toolbar.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = username
            setDisplayHomeAsUpEnabled(true)
        }

        followersPage = createPage(R.string.observations_empty_followers, UsersAdapter()) { page ->
            profileApi.getFollowers(username, page)
        }
        followingUsersPage = createPage(R.string.observations_empty_users, UsersAdapter()) { page ->
            profileApi.getFollowing(username, page)
        }
        // Uzytkownik moze ukryc obserwowane tagi - API odpowiada wtedy 400.
        followingTagsPage =
            createPage(
                emptyText = R.string.observations_empty_tags,
                adapter = TagsAdapter(),
                badRequestText = R.string.observations_tags_private,
            ) { page ->
                profileApi.getObservedTags(username, page)
            }

        binding.tabLayout.apply {
            addTab(newTab().setText(R.string.observations_followers))
            addTab(newTab().setText(R.string.observations_following))
        }
        binding.subTabLayout.apply {
            addTab(newTab().setText(R.string.observations_users))
            addTab(newTab().setText(R.string.observations_tags))
        }
        binding.tabLayout.addOnTabSelectedListener(onTabSelected { showSelectedPage() })
        binding.subTabLayout.addOnTabSelectedListener(onTabSelected { showSelectedPage() })

        showSelectedPage()
    }

    private fun showSelectedPage() {
        val followingSelected = binding.tabLayout.selectedTabPosition == TAB_FOLLOWING
        binding.subTabLayout.isVisible = followingSelected
        val selected =
            when {
                !followingSelected -> followersPage
                binding.subTabLayout.selectedTabPosition == SUB_TAB_TAGS -> followingTagsPage
                else -> followingUsersPage
            }
        listOf(followersPage, followingUsersPage, followingTagsPage).forEach { page ->
            page.binding.root.isVisible = page === selected
        }
        selected.loadIfNeeded()
    }

    private fun <T> createPage(
        @StringRes emptyText: Int,
        adapter: ObservationsAdapter<T>,
        @StringRes badRequestText: Int = R.string.observations_load_error,
        fetch: suspend (page: Int) -> WykopApiResponseV3<List<T>>,
    ): ObservationsPage<T> {
        val pageBinding = ProfileObservationsPageBinding.inflate(layoutInflater, binding.pagesContainer, true)
        pageBinding.root.isVisible = false
        pageBinding.emptyView.setText(emptyText)
        return ObservationsPage(pageBinding, adapter, badRequestText, fetch)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onTabSelected(action: () -> Unit) =
        object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = action()

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        }

    /** Jedna lista z wlasna paginacja: kolejna strona gdy koniec listy zbliza sie do ekranu. */
    private inner class ObservationsPage<T>(
        val binding: ProfileObservationsPageBinding,
        private val adapter: ObservationsAdapter<T>,
        @StringRes private val badRequestText: Int,
        private val fetch: suspend (page: Int) -> WykopApiResponseV3<List<T>>,
    ) {
        private var nextPage: Int? = 1
        private var isLoading = false
        private val layoutManager = LinearLayoutManager(this@ProfileObservationsActivity)

        init {
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.adapter = adapter
            binding.recyclerView.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(
                        recyclerView: RecyclerView,
                        dx: Int,
                        dy: Int,
                    ) = loadIfNeeded()
                },
            )
        }

        fun loadIfNeeded() {
            val page = nextPage ?: return
            if (isLoading) return
            val last = layoutManager.findLastVisibleItemPosition()
            if (adapter.itemCount > 0 && last < adapter.itemCount - LOAD_MORE_THRESHOLD) return
            isLoading = true
            binding.loadingView.isVisible = adapter.itemCount == 0
            binding.emptyView.isVisible = false
            lifecycleScope.launch {
                try {
                    val response = fetch(page)
                    val items = response.data.orEmpty()
                    val perPage = response.pagination?.perPage
                    val total = response.pagination?.total
                    adapter.append(items)
                    nextPage =
                        when {
                            items.isEmpty() -> null
                            total != null && adapter.itemCount >= total -> null
                            perPage != null && perPage > 0 && items.size < perPage -> null
                            else -> page + 1
                        }
                    binding.emptyView.isVisible = adapter.itemCount == 0
                } catch (failure: CancellationException) {
                    throw failure
                } catch (failure: Exception) {
                    Napier.w("Nie udalo sie wczytac strony $page obserwacji", failure)
                    if (adapter.itemCount == 0) {
                        val isBadRequest = (failure as? HttpException)?.code() == HTTP_BAD_REQUEST
                        binding.emptyView.setText(if (isBadRequest) badRequestText else R.string.observations_load_error)
                        binding.emptyView.isVisible = true
                    }
                    // nextPage bez zmian - kolejny scroll sprobuje ponownie.
                } finally {
                    isLoading = false
                    binding.loadingView.isVisible = false
                }
                // Strona nie wypelnila ekranu - scroll nie odpali, dociagamy od razu.
                binding.recyclerView.post { if (nextPage != null && !isLoading && adapter.itemCount > 0) loadIfNeeded() }
            }
        }
    }

    private abstract class ObservationsAdapter<T> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        protected val items = mutableListOf<T>()

        override fun getItemCount() = items.size

        fun append(newItems: List<T>) {
            val start = items.size
            items += newItems
            notifyItemRangeInserted(start, newItems.size)
        }
    }

    private class UsersAdapter : ObservationsAdapter<UserShortResponseV3>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ) = object : RecyclerView.ViewHolder(
            ConversationListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).root,
        ) {}

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            val binding = ConversationListItemBinding.bind(holder.itemView)
            val author = AuthorMapperV3.map(items[position])
            binding.authorAvatarView.setAuthor(author)
            binding.userNameTextView.apply {
                text = author.nick
                setTextColor(context.getGroupColor(author.group))
            }
            binding.lastMessageTextView.isVisible = false
            binding.entryDateTextView.isVisible = false
            binding.root.setOnClickListener {
                it.context.startActivity(ProfileActivity.createIntent(it.context, author.nick))
            }
        }
    }

    private class TagsAdapter : ObservationsAdapter<ShortTagResponseV3>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ) = object : RecyclerView.ViewHolder(
            ProfileObservedTagItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).root,
        ) {}

        override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
        ) {
            val tag = items[position].name
            ProfileObservedTagItemBinding.bind(holder.itemView).tagTextView.apply {
                text = "#$tag"
                setOnClickListener { context.startActivity(TagActivity.createIntent(context, tag)) }
            }
        }
    }

    companion object {
        private const val TAB_FOLLOWING = 1
        private const val SUB_TAB_TAGS = 1
        private const val LOAD_MORE_THRESHOLD = 5

        private const val EXTRA_USERNAME = "EXTRA_USERNAME"

        fun createIntent(
            context: Context,
            username: String,
        ) = Intent(context, ProfileObservationsActivity::class.java).putExtra(EXTRA_USERNAME, username)
    }
}

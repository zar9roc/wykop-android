package io.github.wykopmobilny.ui.modules.mikroblog.feed.hot.v2

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wykopmobilny.databinding.ProgressItemBinding
import io.github.wykopmobilny.models.dataclass.Entry
import io.github.wykopmobilny.storage.api.SettingsPreferencesApi
import io.github.wykopmobilny.ui.adapters.viewholders.BlockedViewHolder
import io.github.wykopmobilny.ui.adapters.viewholders.EntryViewHolder
import io.github.wykopmobilny.ui.fragments.entries.EntryActionListener
import io.github.wykopmobilny.ui.modules.NewNavigator
import io.github.wykopmobilny.utils.layoutInflater
import io.github.wykopmobilny.utils.linkhandler.WykopLinkHandler
import io.github.wykopmobilny.utils.usermanager.UserManagerApi

/**
 * Adapter feedu mikrobloga (V2). Reużywa sprawdzony EntryViewHolder (renderowanie
 * jak dotąd), a paginacja/refresh idą z domeny - stąd własny prosty adapter z listą
 * + stopką ładowania, zamiast starego EndlessProgressAdapter. replyListener=null =>
 * EntryViewHolder w trybie feedu (klik wiersza otwiera szczegóły, brak przycisku
 * odpowiedzi), dokładnie jak stary EntriesAdapter.
 */
internal class MicroblogFeedAdapter(
    private val userManagerApi: UserManagerApi,
    settingsPreferencesApi: SettingsPreferencesApi,
    private val navigator: NewNavigator,
    private val linkHandler: WykopLinkHandler,
    private val entryActionListener: EntryActionListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val cutLongEntries by lazy { settingsPreferencesApi.cutLongEntries }
    private val openSpoilersDialog by lazy { settingsPreferencesApi.openSpoilersDialog }
    private val enableYoutubePlayer by lazy { settingsPreferencesApi.enableYoutubePlayer }
    private val enableEmbedPlayer by lazy { settingsPreferencesApi.enableEmbedPlayer }
    private val showAdultContent by lazy { settingsPreferencesApi.showAdultContent }
    private val hideNsfw by lazy { settingsPreferencesApi.hideNsfw }

    private val entries = mutableListOf<Entry>()
    private var showFooterLoading = false

    fun replaceAll(
        newEntries: List<Entry>,
        footerLoading: Boolean,
    ) {
        entries.clear()
        entries.addAll(newEntries)
        showFooterLoading = footerLoading
        notifyDataSetChanged()
    }

    fun append(newEntries: List<Entry>) {
        if (newEntries.isEmpty()) return
        val insertAt = entries.size
        entries.addAll(newEntries)
        notifyItemRangeInserted(insertAt, newEntries.size)
    }

    fun setFooterLoading(loading: Boolean) {
        if (showFooterLoading == loading) return
        showFooterLoading = loading
        if (loading) {
            notifyItemInserted(entries.size)
        } else {
            notifyItemRemoved(entries.size)
        }
    }

    fun updateEntry(entry: Entry) {
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            entries[index] = entry
            notifyItemChanged(index)
        }
    }

    private fun isFooterPosition(position: Int) = showFooterLoading && position == entries.size

    override fun getItemCount(): Int = entries.size + if (showFooterLoading) 1 else 0

    override fun getItemViewType(position: Int): Int =
        if (isFooterPosition(position)) {
            TYPE_LOADING
        } else {
            EntryViewHolder.getViewTypeForEntry(entries[position])
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_LOADING -> LoadingViewHolder(ProgressItemBinding.inflate(parent.layoutInflater, parent, false))

            EntryViewHolder.TYPE_BLOCKED -> BlockedViewHolder.inflateView(parent, ::notifyItemChanged)

            else ->
                EntryViewHolder.inflateView(
                    parent = parent,
                    viewType = viewType,
                    userManagerApi = userManagerApi,
                    navigator = navigator,
                    linkHandler = linkHandler,
                    entryActionListener = entryActionListener,
                    replyListener = null,
                )
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        when (holder) {
            is EntryViewHolder ->
                holder.bindView(
                    entry = entries[position],
                    cutLongEntries = cutLongEntries,
                    openSpoilersDialog = openSpoilersDialog,
                    enableYoutubePlayer = enableYoutubePlayer,
                    enableEmbedPlayer = enableEmbedPlayer,
                    showAdultContent = showAdultContent,
                    hideNsfw = hideNsfw,
                )

            is BlockedViewHolder -> holder.bindView(entries[position])
        }
    }

    private class LoadingViewHolder(
        binding: ProgressItemBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_LOADING = 100
    }
}

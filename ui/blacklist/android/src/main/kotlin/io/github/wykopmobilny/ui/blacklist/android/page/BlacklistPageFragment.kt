package io.github.wykopmobilny.ui.blacklist.android.page

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.wykopmobilny.ui.blacklist.BlacklistDependencies
import io.github.wykopmobilny.ui.blacklist.BlacklistedDetailsUi
import io.github.wykopmobilny.ui.blacklist.GetBlacklistDetails
import io.github.wykopmobilny.ui.blacklist.android.R
import io.github.wykopmobilny.ui.blacklist.android.databinding.FragmentPageBinding
import io.github.wykopmobilny.utils.requireDependency
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal enum class BlacklistPageType { USERS, DOMAINS, TAGS }

internal class BlacklistPageFragment : Fragment(R.layout.fragment_page) {
    lateinit var getBlacklistDetails: GetBlacklistDetails
    private var currentAdd: BlacklistedDetailsUi.AddUi? = null
    private var suggestJob: Job? = null

    private val pageType by lazy {
        BlacklistPageType.valueOf(requireArguments().getString(ARG_TYPE) ?: BlacklistPageType.USERS.name)
    }

    override fun onAttach(context: Context) {
        getBlacklistDetails = context.requireDependency<BlacklistDependencies>().blacklistDetails()
        super.onAttach(context)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPageBinding.bind(view)

        binding.list.layoutManager = LinearLayoutManager(view.context)
        binding.list.addItemDecoration(DividerItemDecoration(view.context, LinearLayoutManager.VERTICAL))
        val adapter = BlacklistPageAdapter()
        binding.list.adapter = adapter

        binding.inputLayout.hint =
            getString(
                when (pageType) {
                    BlacklistPageType.USERS -> R.string.blacklist_add_user_hint
                    BlacklistPageType.DOMAINS -> R.string.blacklist_add_domain_hint
                    BlacklistPageType.TAGS -> R.string.blacklist_add_tag_hint
                },
            )
        val suggestionAdapter = SuggestionsArrayAdapter(view.context)
        binding.input.setAdapter(suggestionAdapter)

        fun submit() {
            val text = binding.input.text?.toString().orEmpty().trim()
            if (text.isNotBlank()) {
                currentAdd?.submit(text)
                binding.input.setText("")
                suggestionAdapter.clear()
            }
        }
        binding.btnAdd.setOnClickListener { submit() }
        binding.input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        binding.input.doAfterTextChanged { editable ->
            val suggestions = currentAdd?.suggestions ?: return@doAfterTextChanged // domeny: bez podpowiedzi
            val query = editable?.toString().orEmpty().trim().removePrefix("@").removePrefix("#")
            suggestJob?.cancel()
            if (query.length < 2) return@doAfterTextChanged
            suggestJob =
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(SUGGESTION_DEBOUNCE_MS)
                    runCatching { suggestions(query) }
                        .onSuccess { result ->
                            suggestionAdapter.setData(result)
                            if (result.isNotEmpty() && binding.input.hasFocus()) {
                                binding.input.showDropDown()
                            }
                        }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                getBlacklistDetails()
                    .map { ui ->
                        when (pageType) {
                            BlacklistPageType.USERS -> ui.users
                            BlacklistPageType.DOMAINS -> ui.domains
                            BlacklistPageType.TAGS -> ui.tags
                        }
                    }.collect { page ->
                        currentAdd = page.add
                        binding.swipeRefresh.isRefreshing = page.isLoading
                        binding.swipeRefresh.setOnRefreshListener { page.refreshAction() }
                        binding.addProgress.isVisible = page.add.inProgress
                        binding.btnAdd.isVisible = !page.add.inProgress
                        adapter.submitList(page.elements)
                        binding.emptyView.isVisible = !page.isLoading && page.elements.isEmpty()
                    }
            }
        }
    }

    companion object {
        private const val ARG_TYPE = "type"
        private const val SUGGESTION_DEBOUNCE_MS = 300L

        fun newInstance(type: BlacklistPageType) =
            BlacklistPageFragment().apply {
                arguments = bundleOf(ARG_TYPE to type.name)
            }
    }
}

// Podpowiedzi pochodza z serwera (juz dopasowane do zapytania), wiec lokalny filtr
// ArrayAdaptera musi byc wylaczony - inaczej odrzucalby wyniki i dropdown sie nie pokazywal.
// Dane ustawiamy recznie przez setData(), a dropdown pokazujemy wywolaniem showDropDown().
private class SuggestionsArrayAdapter(
    context: Context,
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {
    private val passthroughFilter =
        object : Filter() {
            override fun performFiltering(constraint: CharSequence?) = FilterResults()

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?,
            ) = Unit
        }

    override fun getFilter(): Filter = passthroughFilter

    fun setData(items: List<String>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }
}

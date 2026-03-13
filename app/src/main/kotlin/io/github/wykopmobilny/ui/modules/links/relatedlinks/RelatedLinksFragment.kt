package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.databinding.FragmentRelatedLinksBinding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsComponent
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.utils.InjectableViewModel
import io.github.wykopmobilny.utils.longArgument
import io.github.wykopmobilny.utils.viewModelWrapperFactoryKeyed
import kotlinx.coroutines.launch

class RelatedLinksFragment : Fragment() {
    var linkId by longArgument("linkId")

    private val key: LinkDetailsKey
        get() = LinkDetailsKey(linkId = linkId, initialCommentId = null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentRelatedLinksBinding.inflate(inflater, container, false)

        val viewModel by viewModels<InjectableViewModel<LinkDetailsComponent>> {
            viewModelWrapperFactoryKeyed<LinkDetailsKey, LinkDetailsComponent>(key = key)
        }
        val getRelatedLinks = viewModel.dependency.getRelatedLinks()

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RelatedLinksAdapter()
        binding.recyclerView.adapter = adapter

        // Disable swipe refresh for now (data loaded automatically via InitializeLinkDetails)
        binding.swiperefresh.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                getRelatedLinks().collect { relatedLinks ->
                    adapter.submitList(relatedLinks)

                    if (relatedLinks.isEmpty()) {
                        binding.emptyStateText.isVisible = true
                        binding.emptyStateText.text = "Brak powiązanych linków"
                    } else {
                        binding.emptyStateText.isVisible = false
                        val voteSummary = relatedLinks.mapIndexed { idx, link ->
                            "#$idx ${link.title.take(20)}:${link.upvotesCount.count}" +
                                "(up=${link.upvotesCount.upvoteAction != null}" +
                                ",down=${link.upvotesCount.downvoteAction != null})"
                        }.joinToString(separator = " | ")
                        DiagnosticCheckpoint.log(
                            "RelatedLinks",
                            "Loaded ${relatedLinks.size} items for linkId=$linkId [$voteSummary]",
                        )
                    }
                }
            }
        }

        DiagnosticCheckpoint.log(
            "RelatedLinks",
            "Related links screen opened for linkId=$linkId",
        )
        Napier.i("RelatedLinksFragment opened for linkId=$linkId")

        return binding.root
    }

    companion object {
        fun newInstance(linkId: Long): RelatedLinksFragment =
            RelatedLinksFragment().apply {
                this.linkId = linkId
            }
    }
}

package io.github.wykopmobilny.ui.modules.links.relatedlinks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.databinding.FragmentRelatedLinksBinding
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
import io.github.wykopmobilny.links.details.RelatedLinkUi
import io.github.wykopmobilny.utils.longArgument

class RelatedLinksFragment : Fragment() {
    var linkId by longArgument("linkId")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentRelatedLinksBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RelatedLinksAdapter()
        binding.recyclerView.adapter = adapter

        // TODO: Implement full data loading from RelatedLinksStore
        // For now, show empty list as placeholder
        adapter.submitList(emptyList<RelatedLinkUi>())
        binding.swiperefresh.isRefreshing = false

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

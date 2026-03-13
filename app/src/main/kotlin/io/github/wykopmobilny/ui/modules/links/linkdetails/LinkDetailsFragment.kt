package io.github.wykopmobilny.ui.modules.links.linkdetails

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import com.github.wykopmobilny.ui.components.utils.dpToPx
import io.github.aakira.napier.Napier
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.ActivityLinkDetailsBinding
import io.github.wykopmobilny.kotlin.AppDispatchers
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsComponent
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.links.details.LinkDetailsHeaderUi
import io.github.wykopmobilny.ui.base.components.ContextMenuOptionUi
import io.github.wykopmobilny.utils.InjectableViewModel
import io.github.wykopmobilny.utils.bindings.bindBackButton
import io.github.wykopmobilny.utils.bindings.collectErrorDialog
import io.github.wykopmobilny.utils.bindings.collectInfoDialog
import io.github.wykopmobilny.utils.bindings.collectOptionPicker
import io.github.wykopmobilny.utils.bindings.collectSnackbar
import io.github.wykopmobilny.utils.bindings.collectSwipeRefresh
import io.github.wykopmobilny.utils.bindings.drawableRes
import io.github.wykopmobilny.utils.longArgument
import io.github.wykopmobilny.utils.longArgumentNullable
import io.github.wykopmobilny.utils.viewModelWrapperFactoryKeyed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Suppress("InjectDispatcher")
internal class LinkDetailsFragment : Fragment(R.layout.activity_link_details) {
    private var sortClickAction: (() -> Unit)? = null

    var linkId by longArgument("linkId")
    var commentId by longArgumentNullable("commentId")

    private val key
        get() =
            LinkDetailsKey(
                linkId = linkId,
                initialCommentId = commentId,
            )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val viewModel by viewModels<InjectableViewModel<LinkDetailsComponent>> {
            viewModelWrapperFactoryKeyed<LinkDetailsKey, LinkDetailsComponent>(key = key)
        }
        val getLinkDetails = viewModel.dependency.getLinkDetails()
        val binding = ActivityLinkDetailsBinding.bind(view)
        val toolbar = binding.toolbar.root as Toolbar
        toolbar.bindBackButton(activity = activity)
        setupSortMenu(toolbar)

        val adapter = LinkDetailsAdapterV3()
        adapter.stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        (binding.recyclerView.itemAnimator as? DefaultItemAnimator)?.let { animator ->
            animator.moveDuration =
                (resources.getInteger(android.R.integer.config_shortAnimTime) / 1.5).toLong()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val shared =
                    getLinkDetails()
                        .flowOn(AppDispatchers.Default)
                        .stateIn(this)

                launch {
                    val adapterList =
                        shared
                            .map { it.toAdapterListV3() }
                            .flowOn(AppDispatchers.Default)

                    val targetCommentId = commentId
                    if (savedInstanceState == null && targetCommentId != null) {
                        try {
                            withTimeout(3000) {
                                val state = adapterList.stateIn(this)
                                val targetElement =
                                    state
                                        .mapNotNull { list ->
                                            list
                                                .indexOfFirst { item ->
                                                    when (item) {
                                                        is LinkDetailsListItem.Header,
                                                        is LinkDetailsListItem.RelatedSection,
                                                        -> {
                                                            false
                                                        }

                                                        is LinkDetailsListItem.ParentComment -> {
                                                            item.id == targetCommentId
                                                        }

                                                        is LinkDetailsListItem.ReplyComment -> {
                                                            item.id == targetCommentId
                                                        }
                                                    }
                                                }.takeIf { it >= 0 }
                                        }.first()
                                adapter.submitList(state.value) {
                                    val layoutManager =
                                        checkNotNull(binding.recyclerView.layoutManager)
                                            as LinearLayoutManager
                                    layoutManager.scrollToPositionWithOffset(
                                        targetElement,
                                        8.dpToPx(resources),
                                    )
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (
                            @Suppress("TooGenericExceptionCaught") e: Exception,
                        ) {
                            Napier.w("Couldn't find target comment key=$key", e)
                        }
                    }
                    adapterList.collect { adapter.submitList(it) }
                }
                launch {
                    shared.map { it.errorDialog }.collectErrorDialog(view.context)
                }
                launch {
                    shared.map { it.infoDialog }.collectInfoDialog(view.context)
                }
                launch {
                    shared.map { it.swipeRefresh }.collectSwipeRefresh(binding.swiperefresh)
                }
                launch {
                    shared.map { it.picker }.collectOptionPicker(view.context)
                }
                launch {
                    shared.map { it.snackbar }.collectSnackbar(view)
                }
                launch {
                    shared.collect { ui ->
                        bindContextMenu(toolbar, ui.contextMenuOptions)
                        val header = ui.header
                        if (header is LinkDetailsHeaderUi.WithData) {
                            bindSortMenu(header)
                        }
                    }
                }
            }
        }
    }

    private fun bindContextMenu(
        toolbar: Toolbar,
        options: List<ContextMenuOptionUi>,
    ) {
        val menu = toolbar.menu
        // Keep sort menu item, clear only dynamic context menu items
        val itemsToRemove = mutableListOf<MenuItem>()
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId != SORT_MENU_ID) {
                itemsToRemove.add(item)
            }
        }
        for (item in itemsToRemove) {
            menu.removeItem(item.itemId)
        }

        for (menuOption in options) {
            val item = menu.add(Menu.NONE, Menu.NONE, Menu.NONE, menuOption.label)
            item.setOnMenuItemClickListener {
                menuOption.onClick()
                true
            }
            val iconRes = menuOption.icon?.drawableRes
            if (iconRes != null) {
                item.setIcon(iconRes)
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
        }
    }

    private fun setupSortMenu(toolbar: Toolbar) {
        toolbar.menu
            .add(Menu.NONE, SORT_MENU_ID, Menu.NONE, "Sortuj")
            .setIcon(io.github.wykopmobilny.ui.base.android.R.drawable.ic_sort)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == SORT_MENU_ID) {
                sortClickAction?.invoke()
                true
            } else {
                false
            }
        }
    }

    private fun bindSortMenu(header: LinkDetailsHeaderUi.WithData) {
        sortClickAction = header.commentsSort.clickAction
    }

    companion object {
        private const val SORT_MENU_ID = 9999

        fun newInstance(
            linkId: Long,
            commentId: Long?,
        ): LinkDetailsFragment =
            LinkDetailsFragment().apply {
                this.linkId = linkId
                this.commentId = commentId
            }
    }
}

package io.github.wykopmobilny.ui.modules.links.linkdetails

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import io.github.wykopmobilny.api.suggest.SuggestApi
import io.github.wykopmobilny.api.links.LinksApi
import io.github.wykopmobilny.models.dataclass.LinkCommentV3Item
import io.github.wykopmobilny.ui.widgets.InputToolbar
import io.github.wykopmobilny.ui.widgets.InputToolbarListener
import io.github.wykopmobilny.api.WykopImageFile
import io.github.wykopmobilny.utils.usermanager.UserManagerApi
import io.github.wykopmobilny.utils.usermanager.isUserAuthorized
import javax.inject.Inject
import kotlinx.coroutines.rx2.await
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
import io.github.wykopmobilny.debug.DiagnosticCheckpoint
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("InjectDispatcher")
internal class LinkDetailsFragment : Fragment(R.layout.activity_link_details) {
    private var sortClickAction: (() -> Unit)? = null

    @Inject
    lateinit var userManagerApi: UserManagerApi

    @Inject
    lateinit var suggestApi: SuggestApi

    @Inject
    lateinit var linksApi: LinksApi

    private var inputToolbar: InputToolbar? = null
    private var commentsRefreshAction: (() -> Unit)? = null
    private var cameraPhotoUri: Uri? = null

    private val galleryPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { inputToolbar?.setPhoto(it) }
        }
    private val cameraCapture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            if (saved) cameraPhotoUri?.let { inputToolbar?.setPhoto(it) }
        }

    var linkId by longArgument("linkId")
    var commentId by longArgumentNullable("commentId")

    private val key
        get() =
            LinkDetailsKey(
                linkId = linkId,
                initialCommentId = commentId,
            )

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

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
        val adapter = setupCommentInput(binding)
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
                        // Osobna korutyna - finder nie moze blokowac renderowania listy
                        // (komentarze z odpowiedziami doplywaja progresywnie, docelowy
                        // watek moze pojawic sie dopiero po kilkunastu sekundach).
                        // withTimeoutOrNull + first() zamiast stateIn: stateIn odpala w scope
                        // niekonczaca sie korutyne, przez co withTimeout ZAWSZE konczyl sie
                        // TimeoutCancellationException i ubijal caly kolektor listy.
                        launch {
                            val target =
                                withTimeoutOrNull(SCROLL_TO_COMMENT_TIMEOUT_MS) {
                                    adapterList
                                        .mapNotNull { list ->
                                            list
                                                .indexOfFirst { item ->
                                                    when (item) {
                                                        is LinkDetailsListItem.Header -> {
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
                                                ?.let { position -> position to list }
                                        }.first()
                                }
                            if (target != null) {
                                val (position, list) = target
                                // Jednorazowa nawigacja: bez wyczyszczenia argumentu kazdy powrot
                                // do widoku (repeatOnLifecycle RESUMED) scrollowalby od nowa.
                                commentId = null
                                adapter.submitList(list) {
                                    val layoutManager =
                                        checkNotNull(binding.recyclerView.layoutManager)
                                            as LinearLayoutManager
                                    layoutManager.scrollToPositionWithOffset(
                                        position,
                                        8.dpToPx(resources),
                                    )
                                    DiagnosticCheckpoint.log(
                                        "LinkDetails",
                                        "Scrolled to comment: commentId=$targetCommentId, position=$position",
                                    )
                                }
                            } else {
                                Napier.w("Couldn't find target comment key=$key within timeout")
                            }
                        }
                    }
                    adapterList.collect {
                        adapter.submitList(it)
                        DiagnosticCheckpoint.log(
                            "LinkDetails",
                            "Adapter list updated: ${it.size} items",
                        )
                    }
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
                        commentsRefreshAction = ui.swipeRefresh.refreshAction
                        bindContextMenu(toolbar, ui.contextMenuOptions)
                        val header = ui.header
                        if (header is LinkDetailsHeaderUi.WithData) {
                            bindSortMenu(header)
                            DiagnosticCheckpoint.log(
                                "LinkDetails",
                                "Header loaded: title=${header.title}, votes=${header.voteCount.count}, " +
                                    "comments=${header.commentsCount.label}",
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Wiaze InputToolbar (pole odpowiedzi) i tworzy adapter z akcjami
     * Odpowiedz/Cytat. Dla niezalogowanych pole jest ukryte, a przyciski
     * w komentarzach niewidoczne (callbacki = null).
     */
    private fun setupCommentInput(binding: ActivityLinkDetailsBinding): LinkDetailsAdapterV3 {
        val toolbar = binding.inputToolbar
        inputToolbar = toolbar
        if (!userManagerApi.isUserAuthorized()) {
            toolbar.hide()
            return LinkDetailsAdapterV3()
        }
        toolbar.setup(userManagerApi, suggestApi)
        toolbar.inputToolbarListener =
            object : InputToolbarListener {
                override fun sendPhoto(
                    photo: WykopImageFile,
                    body: String,
                    containsAdultContent: Boolean,
                ) = sendComment(body, containsAdultContent) {
                    linksApi.commentAdd(body, containsAdultContent, photo, linkId)
                }

                override fun sendPhoto(
                    photo: String?,
                    body: String,
                    containsAdultContent: Boolean,
                ) = sendComment(body, containsAdultContent) {
                    linksApi.commentAdd(body, photo, containsAdultContent, linkId)
                }

                override fun openGalleryImageChooser() {
                    galleryPicker.launch("image/*")
                }

                override fun openCamera(uri: Uri) {
                    cameraPhotoUri = uri
                    cameraCapture.launch(uri)
                }
            }
        return LinkDetailsAdapterV3(
            onReplyComment = { author -> toolbar.addAddressant(author) },
            onQuoteComment = { author, body -> toolbar.addQuoteText(body, author) },
        )
    }

    private fun sendComment(
        body: String,
        containsAdultContent: Boolean,
        request: () -> io.reactivex.Single<LinkCommentV3Item>,
    ) {
        if (body.isBlank()) return
        val toolbar = inputToolbar ?: return
        toolbar.showProgress(true)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { request().subscribeOn(io.reactivex.schedulers.Schedulers.io()).await() }
                .onSuccess {
                    toolbar.resetState()
                    // Odswiezenie listy przez akcje domeny (ta sama co swipe-refresh).
                    commentsRefreshAction?.invoke()
                }.onFailure { failure ->
                    toolbar.showProgress(false)
                    Napier.w("Nie udalo sie dodac komentarza", failure)
                    android.widget.Toast
                        .makeText(requireContext(), "Nie udało się dodać komentarza", android.widget.Toast.LENGTH_SHORT)
                        .show()
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
        // Komentarze + odpowiedzi laduja sie w wielu requestach, a API potrafi
        // przytrzymac pojedynczy request (rate limiting) - scroll odpala sie gdy tylko
        // docelowy watek doplynie, wiec dlugi timeout nie blokuje niczego.
        private const val SCROLL_TO_COMMENT_TIMEOUT_MS = 30_000L

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

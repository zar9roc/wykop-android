package io.github.wykopmobilny.ui.modules.addlink.fragments.confirmdetails

import io.github.wykopmobilny.api.responses.AddLinkPreviewImage
import io.github.wykopmobilny.base.BaseView

interface AddLinkDetailsFragmentView : BaseView {
    fun showImages(images: List<AddLinkPreviewImage>)

    // Znalezisko opublikowane - API v3 nie zwraca obiektu Link, wiec tylko zamykamy ekran.
    fun onLinkPublished()

    fun showImagesLoading(visibility: Boolean)

    fun showLinkUploading(visibility: Boolean)
}

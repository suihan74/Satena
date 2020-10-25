package com.suihan74.satena.scenes.post

/**
 * BookmarkPostViewModelを保有しているViewModelOwner
 */
interface BookmarkPostViewModelOwner {
    companion object {
        /** ViewModelの格納キー */
        const val VIEW_MODEL_BOOKMARK_POST = "VIEW_MODEL_BOOKMARK_POST"
    }

    /** ViewModel */
    val bookmarkPostViewModel : BookmarkPostViewModel
}

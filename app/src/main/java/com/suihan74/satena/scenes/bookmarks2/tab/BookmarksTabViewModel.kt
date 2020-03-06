package com.suihan74.satena.scenes.bookmarks2.tab

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Job

/** タブごとに表示内容を変更するためBookmarksTabViewModelを継承して必要なメソッドを埋める */
abstract class BookmarksTabViewModel : ViewModel() {
    protected lateinit var bookmarksViewModel: BookmarksViewModel
        private set

    protected lateinit var preferences: SafeSharedPreferences<PreferenceKey>
        private set

    /** タブごとに表示するブクマリスト */
    val bookmarks by lazy {
        MutableLiveData<List<Bookmark>>()
    }

    /** サインインしているユーザーのブクマ */
    val signedUserBookmark by lazy {
        MutableLiveData<Bookmark?>()
    }

    /** 初期化 */
    open fun init() {
        bookmarks.observeForever {
            // サインインしているユーザーのブクマを取得する
            bookmarksViewModel.repository.let { repo ->
                val user = repo.userSignedIn
                if (user != null) {
                    signedUserBookmark.postValue(updateSignedUserBookmark(user))
                }
            }
        }
    }
    /** ブクマリストを更新（リスト初期化or引っ張って更新時） */
    abstract fun updateBookmarks() : Job
    /** ブクマリストの次のアイテムを取得（スクロールで追加分を取得時） */
    abstract fun loadNextBookmarks() : Job
    /** サインインしているユーザーのブクマをタブ中のブクマリストから探す */
    open fun updateSignedUserBookmark(user: String) : Bookmark? =
        bookmarks.value?.firstOrNull { it.user == user }

    // Listeners
    /** リストトップまでスクロール */
    private var onScrollToTopListener: (()->Unit)? = null
    /** 表示分のボトムまでスクロール */
    private var onScrollToBottomListener: (()->Unit)? = null
    /** 指定したブクマまでスクロール */
    private var onScrollToBookmarkListener: ((Bookmark)->Unit)? = null

    fun setOnScrollToTopListener(action: (()->Unit)?) {
        onScrollToTopListener = action
    }
    fun setOnScrollToBottomListener(action: (()->Unit)?) {
        onScrollToBottomListener = action
    }
    fun setOnScrollToBookmarkListener(action: ((Bookmark)->Unit)?) {
        onScrollToBookmarkListener = action
    }

    /** リストをトップまでスクロールする */
    fun scrollToTop() =
        onScrollToTopListener?.invoke()

    /** リストをボトムまでスクロールする */
    fun scrollToBottom() =
        onScrollToBottomListener?.invoke()

    /** リストを指定ブクマまでスクロールする */
    fun scrollTo(bookmark: Bookmark) =
        onScrollToBookmarkListener?.invoke(bookmark)


    class Factory (
        private val bookmarksViewModel: BookmarksViewModel,
        private val preferences: SafeSharedPreferences<PreferenceKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            (modelClass.newInstance() as BookmarksTabViewModel).apply {
                bookmarksViewModel = this@Factory.bookmarksViewModel
                preferences = this@Factory.preferences
            } as T
    }
}



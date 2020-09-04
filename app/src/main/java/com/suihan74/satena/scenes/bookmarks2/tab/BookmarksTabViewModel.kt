package com.suihan74.satena.scenes.bookmarks2.tab

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.*
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.AddStarPopupMenu
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.OnError
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.OnSuccess
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** タブごとに表示内容を変更するためBookmarksTabViewModelを継承して必要なメソッドを埋める */
abstract class BookmarksTabViewModel : ViewModel() {
    lateinit var activityViewModel: BookmarksViewModel
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

    /** この画面内からスターを追加するポップアップを使用する */
    val useAddStarPopupMenu by lazy {
        preferences.getBoolean(PreferenceKey.BOOKMARKS_USE_ADD_STAR_POPUP_MENU)
    }

    /** 続きをロードできるか */
    val additionalLoadable: Boolean
        get() = activityViewModel.repository.additionalLoadable

    /** 初期化 */
    open fun init() {
        bookmarks.observeForever {
            // サインインしているユーザーのブクマを取得する
            activityViewModel.repository.let { repo ->
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
    fun loadNextBookmarks(
        onSuccess: OnSuccess<List<Bookmark>>?,
        onError: OnError? = null,
        onFinally: OnFinally?
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            val next = withContext(Dispatchers.Default) {
                updateBookmarks().join()
                loadNextBookmarks()
            }
            onSuccess?.invoke(next)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
        finally {
            onFinally?.invoke()
        }
    }
    protected abstract suspend fun loadNextBookmarks() : List<Bookmark>

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


    /** スターをつけるボタンを設定 */
    fun initializeAddStarButton(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        addStarButton: ImageButton,
        bookmark: Bookmark
    ) {
        if (useAddStarPopupMenu &&
            bookmark.comment.isNotBlank() &&
            activityViewModel.signedIn.value == true
        ) {
            addStarButton.visibility = View.VISIBLE

            val userSignedIn = activityViewModel.repository.userSignedIn
            val userStar = bookmark.starCount?.firstOrNull { it.user == userSignedIn }
            if (userSignedIn != null && userStar != null) {
                addStarButton.setImageResource(R.drawable.ic_add_star_filled)
                addStarButton.setOnLongClickListener {
                    activityViewModel.deleteStarDialog(bookmark, userStar)
                    true
                }
            }
            else {
                addStarButton.setImageResource(R.drawable.ic_add_star)
                addStarButton.setOnLongClickListener(null)
            }
            TooltipCompat.setTooltipText(addStarButton, context.getString(R.string.add_star_popup_desc))

            addStarButton.setOnClickListener {
                val popup = AddStarPopupMenu(context).apply {
                    observeUserStars(
                        lifecycleOwner,
                        activityViewModel.repository.userStarsLiveData
                    )

                    setOnClickAddStarListener { color ->
                        activityViewModel.postStarDialog(bookmark, color, "")
                    }

                    setOnClickPurchaseStarsListener {
                        // カラースター購入ページを開く
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.hatena.ne.jp/shop/star"))
                        context.startActivity(intent)
                        dismiss()
                    }
                }
                popup.showAsDropDown(addStarButton)
            }
        }
        else {
            addStarButton.visibility = View.GONE
        }
    }

    // ------ //

    class Factory (
        private val bookmarksTabType: BookmarksTabType,
        private val bookmarksViewModel: BookmarksViewModel,
        private val preferences: SafeSharedPreferences<PreferenceKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        val key : String by lazy {
            BookmarksActivity.getTabViewModelKey(bookmarksTabType)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            when (bookmarksTabType) {
                BookmarksTabType.POPULAR -> PopularTabViewModel()
                BookmarksTabType.RECENT -> RecentTabViewModel()
                BookmarksTabType.ALL -> AllBookmarksTabViewModel()
                BookmarksTabType.CUSTOM -> CustomTabViewModel()
            }.apply {
                activityViewModel = this@Factory.bookmarksViewModel
                preferences = this@Factory.preferences
            } as T
    }
}



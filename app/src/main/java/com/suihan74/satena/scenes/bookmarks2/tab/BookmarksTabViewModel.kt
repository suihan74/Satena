package com.suihan74.satena.scenes.bookmarks2.tab

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.AddStarPopupMenu
import com.suihan74.satena.scenes.bookmarks2.BookmarksAdapter
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks2.BookmarksViewModel
import com.suihan74.utilities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** タブごとに表示内容を変更するためBookmarksTabViewModelを継承して必要なメソッドを埋める */
abstract class BookmarksTabViewModel(
    val activityViewModel: BookmarksViewModel,
    val preferences: SafeSharedPreferences<PreferenceKey>
) : ViewModel() {
    /** BookmarksAdapterで表示されている内容のキャッシュ */
    var displayStates: List<RecyclerState<BookmarksAdapter.Entity>>? = null

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

    var bookmarksAdapter: BookmarksAdapter? = null

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
//                updateBookmarks().join()
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
    private var onScrollToTopListener: Listener<Unit>? = null
    /** 表示分のボトムまでスクロール */
    private var onScrollToBottomListener: Listener<Unit>? = null
    /** 指定したブクマまでスクロール */
    private var onScrollToBookmarkListener: Listener<Bookmark>? = null

    fun setOnScrollToTopListener(action: Listener<Unit>?) {
        onScrollToTopListener = action
    }
    fun setOnScrollToBottomListener(action: Listener<Unit>?) {
        onScrollToBottomListener = action
    }
    fun setOnScrollToBookmarkListener(action: Listener<Bookmark>?) {
        onScrollToBookmarkListener = action
    }

    /** リストをトップまでスクロールする */
    fun scrollToTop() =
        onScrollToTopListener?.invoke(Unit)

    /** リストをボトムまでスクロールする */
    fun scrollToBottom() =
        onScrollToBottomListener?.invoke(Unit)

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
            val repository = activityViewModel.repository

            addStarButton.visibility = View.VISIBLE

            val userSignedIn = repository.userSignedIn
            val userStars = repository.getStarsEntryTo(bookmark.user)?.allStars?.filter { it.user == userSignedIn }

            if (userSignedIn != null && !userStars.isNullOrEmpty()) {
                addStarButton.setImageResource(R.drawable.ic_add_star_filled)
                addStarButton.setOnLongClickListener {
                    activityViewModel.deleteStarDialog(
                        bookmark,
                        userStars,
                        onSuccess = { context.showToast(R.string.msg_delete_star_succeeded) },
                        onError = { e ->
                            context.showToast(R.string.msg_delete_star_failed)
                            Log.e("DeleteStar", Log.getStackTraceString(e))
                        }
                    )
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
                        repository.userStarsLiveData
                    )

                    setOnClickAddStarListener { color ->
                        activityViewModel.postStarDialog(bookmark, color, "")
                        dismiss()
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

    companion object {
        fun createInstance(
            tabType: BookmarksTabType,
            activityViewModel: BookmarksViewModel,
            prefs: SafeSharedPreferences<PreferenceKey>
        ) : BookmarksTabViewModel = when (tabType) {
            BookmarksTabType.POPULAR -> PopularTabViewModel(activityViewModel, prefs)
            BookmarksTabType.RECENT -> RecentTabViewModel(activityViewModel, prefs)
            BookmarksTabType.ALL -> AllBookmarksTabViewModel(activityViewModel, prefs)
            BookmarksTabType.CUSTOM -> CustomTabViewModel(activityViewModel, prefs)
        }.also {
            it.init()
        }
    }
}



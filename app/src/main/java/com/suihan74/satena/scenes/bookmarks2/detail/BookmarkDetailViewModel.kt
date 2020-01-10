package com.suihan74.satena.scenes.bookmarks2.detail

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.StarColor
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.BookmarksRepository
import com.suihan74.utilities.BookmarkCommentDecorator
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarkDetailViewModel(
    /** BookmarksActivityのViewModel */
    private val bookmarksRepository: BookmarksRepository,
    /** アプリ設定 */
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    /** 表示対象のブックマーク */
    val bookmark: Bookmark
) : ViewModel() {

    /** 詳細画面で表示中のブクマについたスターを監視 */
    val starsToUser = bookmarksRepository.createStarsEntryLiveData(bookmark)

    /** エントリに関する全スター情報を監視 */
    val starsAll = bookmarksRepository.allStarsLiveData

    /** 対象ブクマに対するメンション */
    val mentionsToUser by lazy {
        MutableLiveData<List<StarWithBookmark>>()
    }

    /** 対象ブクマに含まれる他ユーザーへのメンション */
    val mentionsFromUser by lazy {
        MutableLiveData<List<StarWithBookmark>>()
    }

    /** サインインしているユーザーの所持スター情報 */
    val userStars = bookmarksRepository.userStarsLiveData

    /** スターメニューが開いているか */
    val starsMenuOpened by lazy { MutableLiveData<Boolean>() }

    /** スターを付ける際のコメント引用 */
    val quote by lazy { MutableLiveData<String>() }

    // --- Listeners --- //

    /** ブクマに付けられたスター情報のロード失敗時に呼ばれるリスナ */
    private var onLoadedStarsFailureListener : ((Throwable)->Unit)? = null

    /** スター付与に成功した際に呼ばれるリスナ */
    private var onCompletedPostStarListener : ((StarColor)->Unit)? = null

    /** スター付与に失敗した際に呼ばれるリスナ */
    private var onPostStarFailureListener : ((StarColor, Throwable)->Unit)? = null

    fun setOnLoadedStarsFailureListener(listener: ((Throwable)->Unit)? = null) {
        onLoadedStarsFailureListener = listener
    }

    fun setOnCompletedPostStarListener(listener: ((StarColor)->Unit)? = null) {
        onCompletedPostStarListener = listener
    }

    fun setOnPostStarFailureListener(listener: ((StarColor, Throwable)->Unit)? = null) {
        onPostStarFailureListener = listener
    }

    // --- --- //

    /** 初期化 */
    fun init() = viewModelScope.launch {
        starsMenuOpened.postValue(false)
        try {
            userStars.load()
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onLoadedStarsFailureListener?.invoke(e)
            }
        }
    }

    /** メンションリストのロード */
    fun loadMentions() = viewModelScope.launch {
        val displayIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING)
        val ignoredUsers = bookmarksRepository.ignoredUsers
        val bookmarks = bookmarksRepository.bookmarksEntry?.bookmarks ?: emptyList()
        val analyzed = BookmarkCommentDecorator.convert(bookmark.comment)
        mentionsFromUser.postValue(
            analyzed.ids
                .mapNotNull m@ { id ->
                    val b = bookmarks.firstOrNull { it.user == id } ?: return@m null
                    val ignored = ignoredUsers.contains(b.user)
                    val state =
                        if (ignored) {
                            if (displayIgnoredUsers) StarWithBookmark.DisplayState.COVER
                            else return@m null
                        }
                        else StarWithBookmark.DisplayState.SHOW

                    StarWithBookmark(null, b, state)
                }
        )
        mentionsToUser.postValue(
            bookmarks.filter {
                BookmarkCommentDecorator.convert(it.comment)
                    .ids.contains(bookmark.user)
            }.mapNotNull m@ { b ->
                val ignored = ignoredUsers.contains(b.user)
                val state =
                    if (ignored) {
                        if (displayIgnoredUsers) StarWithBookmark.DisplayState.COVER
                        else return@m null
                    }
                    else StarWithBookmark.DisplayState.SHOW

                StarWithBookmark(null, b, state)
            }
        )
    }

    /** userに付いたスターと，それを付けた人の同記事へのブクマを取得する */
    fun getStarsWithBookmarkTo(user: String) : List<StarWithBookmark> {
        val bookmarks = bookmarksRepository.bookmarksEntry?.bookmarks ?: emptyList()
        val recentBookmarks = bookmarksRepository.bookmarksRecent

        val displayIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS)
        val ignoredUsers = bookmarksRepository.ignoredUsers

        return bookmarksRepository.getStarsEntryTo(user)?.allStars?.mapNotNull m@ {
            val bookmark =
                recentBookmarks.firstOrNull { b -> b.user == it.user }
                ?: bookmarks.firstOrNull { b -> b.user == it.user }
                ?: Bookmark(user = it.user, comment = "")

            val ignored = ignoredUsers.contains(bookmark.user)
            val state =
                if (ignored) {
                    if (displayIgnoredUsers) StarWithBookmark.DisplayState.COVER
                    else return@m null
                }
                else StarWithBookmark.DisplayState.SHOW

            StarWithBookmark(it, bookmark, state)
        } ?: emptyList()
    }

    /** userが付けたスターと，そのスターがついたブクマを取得する */
    fun getStarsWithBookmarkFrom(user: String) : List<StarWithBookmark> {
        val bookmarks = bookmarksRepository.bookmarksEntry?.bookmarks ?: emptyList()
        val recentBookmarks = bookmarksRepository.bookmarksRecent

        val displayIgnoredUsers = prefs.getBoolean(PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS)
        val ignoredUsers = bookmarksRepository.ignoredUsers

        return bookmarksRepository.getStarsEntryFrom(user).mapNotNull m@ {
            val bookmark =
                recentBookmarks.firstOrNull { b -> it.url.contains("/${b.user}/") }
                ?: bookmarks.firstOrNull { b -> it.url.contains("/${b.user}/") }
                ?: return@m null

            val star = it.allStars.firstOrNull { s -> s.user == user }
                ?: return@m null

            val ignored = ignoredUsers.contains(bookmark.user)
            val state =
                if (ignored) {
                    if (displayIgnoredUsers) StarWithBookmark.DisplayState.COVER
                    else return@m null
                }
                else StarWithBookmark.DisplayState.SHOW

            StarWithBookmark(star, bookmark, state)
        }
    }

    fun updateStarsToUser(forceUpdate: Boolean = false) = viewModelScope.launch {
        try {
            starsToUser.updateAsync(forceUpdate).await()
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onLoadedStarsFailureListener?.invoke(e)
            }
        }
    }

    fun updateStarsAll(forceUpdate: Boolean) = viewModelScope.launch {
        try {
            starsAll.updateAsync(forceUpdate).await()
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onLoadedStarsFailureListener?.invoke(e)
            }
        }
    }

    /** 対象ブクマにスターを付ける */
    fun postStar(color: StarColor) = viewModelScope.launch {
        if (!checkStarCount(color)) return@launch

        try {
            bookmarksRepository.postStar(bookmark, color, quote.value ?: "")
        }
        catch (e: Throwable) {
            withContext(Dispatchers.Main) {
                onPostStarFailureListener?.invoke(color, e)
            }
            return@launch
        }

        // リストを更新する
        updateStarsToUser(true)

        withContext(Dispatchers.Main) {
            onCompletedPostStarListener?.invoke(color)
        }
    }

    /** 指定カラーのスターを所持しているか確認する */
    fun checkStarCount(color: StarColor) : Boolean {
        try {
            if (!bookmarksRepository.signedIn)
                throw NotSignedInException()

            val activeStars = userStars.value ?: throw StarExhaustedException(color)

            val valid = when (color) {
                StarColor.Red -> activeStars.red > 0
                StarColor.Green -> activeStars.green > 0
                StarColor.Blue -> activeStars.blue > 0
                StarColor.Purple -> activeStars.purple > 0
                else -> true
            }
            if (!valid) {
                throw StarExhaustedException(color)
            }

            return true
        }
        catch (e: Throwable) {
            onPostStarFailureListener?.invoke(color, e)
            return false
        }
    }

    class NotSignedInException : RuntimeException(
        "failed to an action required sign-in."
    )

    class StarExhaustedException(val color : StarColor) : RuntimeException(
        "${color.name} star has been exhausted."
    )

    class Factory(
        private val bookmarksRepository: BookmarksRepository,
        private val prefs: SafeSharedPreferences<PreferenceKey>,
        private val bookmark: Bookmark
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            BookmarkDetailViewModel(bookmarksRepository, prefs, bookmark) as T
    }
}

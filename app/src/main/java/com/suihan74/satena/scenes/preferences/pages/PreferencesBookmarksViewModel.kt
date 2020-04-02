package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesBookmarksViewModel(
    private val prefs: SafeSharedPreferences<PreferenceKey>
) : ViewModel() {
    /** SafeSharedPreferencesと紐づいたLiveDataを作成する */
    private inline fun <reified T> createLiveData(key: PreferenceKey) =
        MutableLiveData<T>(prefs.get<T>(key)).apply {
            observeForever {
                prefs.edit {
                    put(key, it)
                }
            }
        }

    /** 最初に表示するタブのindex */
    val initialTabPosition = createLiveData<Int>(
        PreferenceKey.BOOKMARKS_INITIAL_TAB
    )

    /** ブクマ投稿前に確認ダイアログを表示する */
    val confirmPostBookmark = createLiveData<Boolean>(
        PreferenceKey.USING_POST_BOOKMARK_DIALOG
    )

    /** スター投稿前に確認ダイアログを表示する */
    val confirmPostStar = createLiveData<Boolean>(
        PreferenceKey.USING_POST_STAR_DIALOG
    )

    /** スクロールでツールバーの表示状態を変化させる */
    val toggleToolbarByScrolling = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING
    )

    /** スクロールでボタンの表示状態を変化させる */
    val toggleButtonsByScrolling = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING
    )

    /** 「すべて」タブでは非表示ブクマを表示する */
    val displayMutedBookmarksInAllBookmarksTab = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS
    )

    /** IDコールの言及先の非表示ブクマを表示する */
    val displayMutedBookmarksInMention = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING
    )

    /** 非表示ユーザーのスターを表示する */
    val displayIgnoredUsersStar = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS
    )

    /** リンク部分をタップしたときの動作 */
    val linkSingleTapAction = createLiveData<Int>(
        PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION
    )

    /** リンク部分をロングタップしたときの動作 */
    val linkLongTapAction = createLiveData<Int>(
        PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION
    )

    /** タブ長押しで初期タブを変更する */
    val changeHomeByLongTapping = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_CHANGE_HOME_BY_LONG_TAPPING_TAB
    )

    class Factory(
        private val prefs: SafeSharedPreferences<PreferenceKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            PreferencesBookmarksViewModel(prefs) as T
    }
}

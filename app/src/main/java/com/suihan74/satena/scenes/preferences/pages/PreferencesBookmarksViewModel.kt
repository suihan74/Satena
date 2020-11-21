package com.suihan74.satena.scenes.preferences.pages

import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesBookmarksViewModel(
    prefs: SafeSharedPreferences<PreferenceKey>
) : PreferencesViewModel<PreferenceKey>(prefs) {

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

    /** ブクマ一覧画面の項目に対してスターを付けられるようにする */
    val useAddStarPopupMenu = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_USE_ADD_STAR_POPUP_MENU
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
    val linkSingleTapAction = createLiveDataEnum(
        PreferenceKey.BOOKMARK_LINK_SINGLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** リンク部分をロングタップしたときの動作 */
    val linkLongTapAction = createLiveDataEnum(
        PreferenceKey.BOOKMARK_LINK_LONG_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromId(it) }
    )

    /** タブ長押しで初期タブを変更する */
    val changeHomeByLongTapping = createLiveData<Boolean>(
        PreferenceKey.BOOKMARKS_CHANGE_HOME_BY_LONG_TAPPING_TAB
    )
}

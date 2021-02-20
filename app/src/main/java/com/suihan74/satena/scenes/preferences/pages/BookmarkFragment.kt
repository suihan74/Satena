package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.addPrefItem
import com.suihan74.satena.scenes.preferences.addPrefToggleItem
import com.suihan74.satena.scenes.preferences.addSection

/**
 * 「ブックマーク」画面
 */
class BookmarkFragment : ListPreferencesFragment() {
    override val viewModel by lazy {
        BookmarkViewModel(requireContext())
    }
}

// ------ //

class BookmarkViewModel(context: Context) : ListPreferencesViewModel(context) {
    /** 最初に表示するタブのindex */
    val initialTabPosition = createLiveDataEnum(
        PreferenceKey.BOOKMARKS_INITIAL_TAB,
        { it.ordinal },
        { BookmarksTabType.fromOrdinal(it) }
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

    // ------ //

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment): List<PreferencesAdapter.Item> = buildList {
        val fragmentManager = fragment.childFragmentManager

        addSection(R.string.pref_bookmark_section_tab)
        addPrefItem(fragment, initialTabPosition, R.string.pref_bookmarks_initial_tab_desc) {
            openEnumSelectionDialog(
                BookmarksTabType.values(),
                initialTabPosition,
                R.string.pref_bookmarks_initial_tab_desc,
                fragmentManager
            )
        }
        addPrefToggleItem(fragment, changeHomeByLongTapping, R.string.pref_bookmarks_change_home_by_long_tapping_desc)

        // --- //

        addSection(R.string.pref_bookmark_section_dialog)
        addPrefToggleItem(fragment, confirmPostBookmark, R.string.pref_bookmarks_using_post_dialog_desc)
        addPrefToggleItem(fragment, confirmPostStar, R.string.pref_bookmarks_using_post_star_dialog_desc)

        // --- //

        addSection(R.string.pref_bookmark_section_behavior)
        addPrefToggleItem(fragment, useAddStarPopupMenu, R.string.pref_bookmarks_using_add_star_popup_menu_desc)
        addPrefToggleItem(fragment, toggleToolbarByScrolling, R.string.pref_bookmarks_hiding_toolbar_by_scrolling)
        addPrefToggleItem(fragment, toggleButtonsByScrolling, R.string.pref_bookmarks_hiding_buttons_with_scrolling_desc)

        // --- //

        addSection(R.string.pref_bookmark_section_ignoring)
        addPrefToggleItem(fragment, displayMutedBookmarksInAllBookmarksTab, R.string.pref_bookmarks_showing_ignored_users_in_all_bookmarks_desc)
        addPrefToggleItem(fragment, displayMutedBookmarksInMention, R.string.pref_bookmarks_showing_ignored_users_with_calling_desc)
        addPrefToggleItem(fragment, displayIgnoredUsersStar, R.string.pref_bookmarks_showing_stars_of_ignored_users_desc)

        // --- //

        addSection(R.string.pref_bookmark_section_link)
        addPrefItem(fragment, linkSingleTapAction, R.string.pref_bookmark_link_single_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                linkSingleTapAction,
                R.string.pref_bookmark_link_single_tap_action_desc,
                fragmentManager
            )
        }
        addPrefItem(fragment, linkLongTapAction, R.string.pref_bookmark_link_long_tap_action_desc) {
            openEnumSelectionDialog(
                TapEntryAction.values(),
                linkLongTapAction,
                R.string.pref_bookmark_link_long_tap_action_desc,
                fragmentManager
            )
        }
    }
}

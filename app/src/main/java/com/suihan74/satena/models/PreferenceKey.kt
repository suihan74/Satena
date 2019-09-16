package com.suihan74.satena.models

import com.suihan74.HatenaLib.Category
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import org.threeten.bp.LocalDateTime
import java.lang.reflect.Type

@SharedPreferencesKey(fileName = "default", version = 1, latest = true)
enum class PreferenceKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    ////////////////////////////////////////
    // accounts
    ////////////////////////////////////////
    ID(typeInfo<String>(), ""),
    HATENA_USER_NAME(typeInfo<String>(), ""),
    HATENA_PASSWORD(typeInfo<String>(), ""),
    MASTODON_ACCESS_TOKEN(typeInfo<String>(), ""),

    ////////////////////////////////////////
    // generals
    ////////////////////////////////////////
    DARK_THEME(typeInfo<Boolean>(), false),
    USING_TERMINATION_DIALOG(typeInfo<Boolean>(), false),
    NOTICES_LAST_SEEN_UPDATABLE(typeInfo<Boolean>(), true),
    BACKGROUND_CHECKING_NOTICES(typeInfo<Boolean>(), true),
    BACKGROUND_CHECKING_NOTICES_INTERVALS(typeInfo<Long>(), 3L),
    NOTICES_LAST_SEEN(typeInfo<LocalDateTime>(), null),

    ////////////////////////////////////////
    // entries
    ////////////////////////////////////////
    ENTRY_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_COMMENTS.int),
    ENTRY_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.int),
    ENTRIES_HOME_CATEGORY(typeInfo<Int>(), Category.All.int),
    ENTRIES_INITIAL_TAB(typeInfo<Int>(), EntriesTabType.POPULAR.int),
    ENTRIES_MENU_TAP_GUARD(typeInfo<Boolean>(), true),
    ENTRIES_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), true),

    ////////////////////////////////////////
    // bookmarks
    ////////////////////////////////////////
    USING_POST_BOOKMARK_DIALOG(typeInfo<Boolean>(), true),
    BOOKMARKS_INITIAL_TAB(typeInfo<Int>(), BookmarksTabType.POPULAR.int),
    BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING(typeInfo<Boolean>(), true),
    BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS(typeInfo<Boolean>(), true),
    BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING(typeInfo<Boolean>(), false),
    BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS(typeInfo<Boolean>(), true),
    BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), false),
    USING_POST_STAR_DIALOG(typeInfo<Boolean>(), true),
    BOOKMARK_LINK_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.int),
    BOOKMARK_LINK_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.int)
}


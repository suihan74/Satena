package com.suihan74.satena.models

import android.content.Context
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import org.threeten.bp.LocalDateTime
import java.lang.reflect.Type

@SharedPreferencesKey(fileName = "default", version = 2, latest = true)
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
    BOOKMARK_LINK_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.int),

    ////////////////////////////////////////
    // custom bookmarks tab
    ////////////////////////////////////////
    CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS(typeInfo<List<Int>>(), emptyList<Int>()),
    CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE(typeInfo<Boolean>(), true),
    CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE(typeInfo<Boolean>(), false),
    CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE(typeInfo<Boolean>(), false)
}

////////////////////////////////////////////////////////////////////////////////
// previous versions
////////////////////////////////////////////////////////////////////////////////

/**************************************
 * version 1 -> 2 での変更
 * Category.GeneralをCategory.AllとCategory.Socialの間に挿入したことによる変更を
 * ENTRIES_HOME_CATEGORYにも適用するための変更
 **************************************/

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

object PreferenceKeyMigrator {
    fun check(context: Context) {
        val version = SafeSharedPreferences.version<PreferenceKey>(context)
        when (version) {
            1 -> migrateFromVersion1(context)
        }
    }

    private fun migrateFromVersion1(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val homeCategory = prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY)
        prefs.edit {
            if (homeCategory > 0) {
                putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, homeCategory + 1)
            }
        }
    }
}

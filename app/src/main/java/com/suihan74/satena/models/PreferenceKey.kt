package com.suihan74.satena.models

import android.content.Context
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import org.threeten.bp.LocalDateTime
import java.lang.reflect.Type

@SharedPreferencesKey(fileName = "default", version = 3, latest = true)
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
    IGNORE_NOTICES_FROM_SPAM(typeInfo<Boolean>(), true),
    APP_VERSION_LAST_LAUNCH(typeInfo<String>(), "0"),
    SHOW_RELEASE_NOTES_AFTER_UPDATE(typeInfo<Boolean>(), true),

    ////////////////////////////////////////
    // entries
    ////////////////////////////////////////
    ENTRY_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_COMMENTS.ordinal),
    ENTRY_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.ordinal),
    ENTRIES_HOME_CATEGORY(typeInfo<Int>(), Category.All.ordinal),
    ENTRIES_INITIAL_TAB(typeInfo<Int>(), EntriesTabType.POPULAR.ordinal),
    ENTRIES_MENU_TAP_GUARD(typeInfo<Boolean>(), true),
    ENTRIES_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), true),

    ////////////////////////////////////////
    // bookmarks
    ////////////////////////////////////////
    USING_POST_BOOKMARK_DIALOG(typeInfo<Boolean>(), true),
    BOOKMARKS_INITIAL_TAB(typeInfo<Int>(), BookmarksTabType.POPULAR.ordinal),
    BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING(typeInfo<Boolean>(), true),
    BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS(typeInfo<Boolean>(), true),
    BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING(typeInfo<Boolean>(), false),
    BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS(typeInfo<Boolean>(), true),
    BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), false),
    USING_POST_STAR_DIALOG(typeInfo<Boolean>(), true),
    BOOKMARK_LINK_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.ordinal),
    BOOKMARK_LINK_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.ordinal),

    ////////////////////////////////////////
    // custom bookmarks tab
    ////////////////////////////////////////
    CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS(typeInfo<List<Int>>(), emptyList<Int>()),
    CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE(typeInfo<Boolean>(), true),
    CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE(typeInfo<Boolean>(), false),
    CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE(typeInfo<Boolean>(), false)
}

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

@Suppress("deprecation")
object PreferenceKeyMigration {
    fun check(context: Context) {
        when (SafeSharedPreferences.version<PreferenceKey>(context)) {
            1 -> migrateFromVersion1(context)
            2 -> migrateFromVersion2(context)
        }
    }

    /**
     * v1 -> v2
     *
     * Category.GeneralをCategory.AllとCategory.Socialの間に挿入
     */
    private fun migrateFromVersion1(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val homeCategoryOrdinal = prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY)
        prefs.edit {
            if (homeCategoryOrdinal > 0) {
                val fixedCategoryOrdinal = homeCategoryOrdinal + 1
                putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, fixedCategoryOrdinal)

                when (Category.fromInt(fixedCategoryOrdinal)) {
                    Category.MyBookmarks ->
                        putInt(PreferenceKey.ENTRIES_INITIAL_TAB, EntriesTabType.MYBOOKMARKS.ordinal)

                    else ->
                        putInt(PreferenceKey.ENTRIES_INITIAL_TAB, EntriesTabType.POPULAR.ordinal)
                }
            }
        }
        // 以下、次のバージョン移行処理
        migrateFromVersion2(context)
    }

    /**
     * v2 -> v3
     *
     * Category.MyTagsをCategory.MyBookmarksに統合した
     */
    private fun migrateFromVersion2(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val homeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        prefs.edit {
            if (homeCategory == Category.MyTags) {
                putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, Category.MyBookmarks.ordinal)
                putInt(PreferenceKey.ENTRIES_INITIAL_TAB, EntriesTabType.MYBOOKMARKS.ordinal)
            }
        }
    }
}

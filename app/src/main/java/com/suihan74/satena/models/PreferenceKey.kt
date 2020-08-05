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

    /** 端末ごとのユニークID */
    ID(typeInfo<String>(), ""),

    /** はてなのログインID(暗号化) */
    HATENA_USER_NAME(typeInfo<String>(), ""),

    /** はてなのパスワード(暗号化) */
    HATENA_PASSWORD(typeInfo<String>(), ""),

    /** Mastodonのアクセストークン(暗号化) */
    MASTODON_ACCESS_TOKEN(typeInfo<String>(), ""),

    ////////////////////////////////////////
    // generals
    ////////////////////////////////////////

    /** ダークテーマを使用 */
    DARK_THEME(typeInfo<Boolean>(), false),

    /** 終了確認ダイアログを表示 */
    USING_TERMINATION_DIALOG(typeInfo<Boolean>(), false),

    /** 通知取得フラグをアプリから更新 */
    NOTICES_LAST_SEEN_UPDATABLE(typeInfo<Boolean>(), true),

    /** バックグラウンドで通知を監視 */
    BACKGROUND_CHECKING_NOTICES(typeInfo<Boolean>(), true),

    /** 通知監視インターバル(分) */
    BACKGROUND_CHECKING_NOTICES_INTERVALS(typeInfo<Long>(), 3L),

    /** 最後に通知を確認した時刻 */
    NOTICES_LAST_SEEN(typeInfo<LocalDateTime>(), null),

    /** スパムと思われる通知を報せない */
    IGNORE_NOTICES_FROM_SPAM(typeInfo<Boolean>(), true),

    /** 最後に起動したときのアプリバージョン */
    APP_VERSION_LAST_LAUNCH(typeInfo<String>(), "0"),

    /** アップデート後最初の起動時にリリースノートを表示する */
    SHOW_RELEASE_NOTES_AFTER_UPDATE(typeInfo<Boolean>(), true),

    ////////////////////////////////////////
    // entries
    ////////////////////////////////////////

    /** エントリ項目シングルタップの挙動 */
    ENTRY_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_COMMENTS.ordinal),

    /** エントリ項目ロングタップの挙動 */
    ENTRY_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.ordinal),

    /** 最初に表示するカテゴリ */
    ENTRIES_HOME_CATEGORY(typeInfo<Int>(), Category.All.ordinal),

    /** 最初に表示するタブ(の位置) */
    ENTRIES_INITIAL_TAB(typeInfo<Int>(), 0),

    /** メニュー展開時にタップ防止用の画面暗転をする */
    ENTRIES_MENU_TAP_GUARD(typeInfo<Boolean>(), true),

    /** コンテンツスクロールにあわせてツールバーを隠す */
    ENTRIES_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), true),

    /** タブ部分をロングタップしてホームカテゴリ・ホームタブを変更できるようにする */
    ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB(typeInfo<Boolean>(), true),

    /** 「あとで読む」エントリを「読んだ」したときの挙動 */
    ENTRY_READ_ACTION_TYPE(typeInfo<Int>(), EntryReadActionType.SILENT_BOOKMARK.ordinal),

    /** EntryReadActionType.BOILERPLATE時の定型文 */
    ENTRY_READ_ACTION_BOILERPLATE(typeInfo<String>(), ""),

    ////////////////////////////////////////
    // bookmarks
    ////////////////////////////////////////

    /** ブクマ登録前に確認ダイアログを表示する */
    USING_POST_BOOKMARK_DIALOG(typeInfo<Boolean>(), true),

    /** 最初に表示するタブ */
    BOOKMARKS_INITIAL_TAB(typeInfo<Int>(), BookmarksTabType.POPULAR.ordinal),

    /** リストスクロールにあわせてメニューボタンを隠す */
    BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING(typeInfo<Boolean>(), true),

    /** 「すべて」タブで非表示ユーザーも表示する */
    BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS(typeInfo<Boolean>(), true),

    /** IDコール先の非表示ユーザーのブクマを表示する */
    BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING(typeInfo<Boolean>(), false),

    /** ブクマ詳細画面で非表示ユーザーのスターをリストに表示する */
    BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS(typeInfo<Boolean>(), true),

    /** リストスクロールにあわせてツールバーを隠す */
    BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), false),

    /** スターを付ける前に確認ダイアログを表示する */
    USING_POST_STAR_DIALOG(typeInfo<Boolean>(), true),

    /** ブコメ中のリンクをシングルタップしたときの挙動 */
    BOOKMARK_LINK_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.ordinal),

    /** ブコメ中のリンクをロングタップしたときの挙動 */
    BOOKMARK_LINK_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.ordinal),

    /** タブ部分をロングタップして最初に表示するタブを変更できるようにする */
    BOOKMARKS_CHANGE_HOME_BY_LONG_TAPPING_TAB(typeInfo<Boolean>(), true),

    ////////////////////////////////////////
    // custom bookmarks tab
    ////////////////////////////////////////

    /** 「カスタム」タブのリストに表示するユーザータグ(ID) */
    CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS(typeInfo<List<Int>>(), emptyList<Int>()),

    /** 「カスタム」タブでユーザータグ未分類のユーザーを表示する */
    CUSTOM_BOOKMARKS_IS_UNAFFILIATED_USERS_ACTIVE(typeInfo<Boolean>(), true),

    /** 「カスタム」タブで無言ブクマを表示する */
    CUSTOM_BOOKMARKS_IS_NO_COMMENT_USERS_ACTIVE(typeInfo<Boolean>(), false),

    /** 「カスタム」タブで非表示ユーザーを表示する */
    CUSTOM_BOOKMARKS_IS_MUTED_USERS_ACTIVE(typeInfo<Boolean>(), false),

    ////////////////////////////////////////
    // fonts
    ////////////////////////////////////////

    /** エントリタイトルのフォントサイズ */
    FONT_ENTRY_TITLE(typeInfo<FontSettings>(), FontSettings("sans-serif", 14f, bold = true)),

    /** ブコメ本文のフォントサイズ */
    FONT_BOOKMARK_COMMENT(typeInfo<FontSettings>(), FontSettings("sans-serif", 14f)),

    /** ブコメユーザー名のフォントサイズ */
    FONT_BOOKMARK_USER(typeInfo<FontSettings>(), FontSettings("sans-serif", 14f, bold = true)),
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
                putInt(PreferenceKey.ENTRIES_INITIAL_TAB, 0)
            }
        }
        // 以下、次のバージョン移行処理
        migrateFromVersion2(context)
    }

    /**
     * v2 -> v3
     *
     * 1. Category.MyTagsをCategory.MyBookmarksに統合した
     * 2. Category.MyStarsとCategory.StarsReportをCategory.Starに統合した (MyStars→Starsにリネーム、StarsReportをdeprecatedに設定)
     * 3. INITIAL_TAB を EntriesTabTypeのordinalではなく、TabIndex(0or1)に変更
     */
    private fun migrateFromVersion2(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val homeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        val initialTab = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)

        prefs.edit {
            when (homeCategory) {
                // 1.
                Category.MyTags -> {
                    putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, Category.MyBookmarks.ordinal)
                    putInt(PreferenceKey.ENTRIES_INITIAL_TAB, 0)
                }

                // 2.
                Category.StarsReport -> {
                    putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, Category.Stars.ordinal)
                }

                else -> { /* do nothing */ }
            }

            // 3.
            putInt(PreferenceKey.ENTRIES_INITIAL_TAB, initialTab % 2)
        }
    }
}

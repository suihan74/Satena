package com.suihan74.satena.models

import android.content.Context
import android.view.Gravity
import com.suihan74.satena.scenes.bookmarks2.BookmarksTabType
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.entries2.CategoriesMode
import com.suihan74.satena.scenes.entries2.ExtraBottomItemsAlignment
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import org.threeten.bp.LocalDateTime
import java.lang.reflect.Type

@SharedPreferencesKey(fileName = "default", version = 4, latest = true)
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

    /** ドロワーの位置 */
    DRAWER_GRAVITY(typeInfo<Int>(), Gravity.RIGHT),

    /** アプリ内アップデート通知 */
    APP_UPDATE_NOTICE_MODE(typeInfo<Int>(), AppUpdateNoticeMode.FIX.int),

    /** 一度無視したアップデートを再度通知する */
    NOTICE_IGNORED_APP_UPDATE(typeInfo<Boolean>(), false),

    /** 最後に通知したアップデートバージョン */
    LAST_NOTICED_APP_UPDATE_VERSION(typeInfo<Long>(), 0L),

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

    /** ボトムバーを使用する */
    ENTRIES_BOTTOM_LAYOUT_MODE(typeInfo<Boolean>(), true),

    /** スクロールでボトムバーを隠す */
    ENTRIES_HIDE_BOTTOM_LAYOUT_BY_SCROLLING(typeInfo<Boolean>(), false),

    /** ボトムバーに表示する項目 */
    ENTRIES_BOTTOM_ITEMS(typeInfo<List<UserBottomItem>>(), listOf(UserBottomItem.SCROLL_TO_TOP)),

    /** ボトムバーの項目を左詰めにするか右詰めにするか */
    ENTRIES_BOTTOM_ITEMS_GRAVITY(typeInfo<Int>(), Gravity.END),

    /** ボトムバーの追加項目の表示位置 */
    ENTRIES_EXTRA_BOTTOM_ITEMS_ALIGNMENT(typeInfo<Int>(), ExtraBottomItemsAlignment.DEFAULT.id),

    /** エントリ項目シングルタップの挙動 */
    ENTRY_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_COMMENTS.id),

    /** エントリ項目複数回タップの挙動 */
    ENTRY_MULTIPLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.id),

    /** エントリ項目ロングタップの挙動 */
    ENTRY_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.id),

    /** タップ回数判定時間 0L~500L */
    ENTRY_MULTIPLE_TAP_DURATION(typeInfo<Long>(), 250L),

    /** 最初に表示するカテゴリ */
    ENTRIES_HOME_CATEGORY(typeInfo<Int>(), Category.All.id),

    /** 最初に表示するタブ(の位置) */
    ENTRIES_INITIAL_TAB(typeInfo<Int>(), 0),

    /** カテゴリリストの表示形式 */
    ENTRIES_CATEGORIES_MODE(typeInfo<Int>(), CategoriesMode.LIST.ordinal),

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

    /** ブクマ一覧画面のリスト項目から直接スターを付けられるようにする */
    BOOKMARKS_USE_ADD_STAR_POPUP_MENU(typeInfo<Boolean>(), true),

    /** スターを付ける前に確認ダイアログを表示する */
    USING_POST_STAR_DIALOG(typeInfo<Boolean>(), true),

    /** ブコメ中のリンクをシングルタップしたときの挙動 */
    BOOKMARK_LINK_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.id),

    /** ブコメ中のリンクをロングタップしたときの挙動 */
    BOOKMARK_LINK_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.id),

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
    // browser
    ////////////////////////////////////////

    /** 使用する内部ブラウザ */
    BROWSER_MODE(typeInfo<Int>(), BrowserMode.WEB_VIEW.id),
}

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

@Suppress("deprecation")
object PreferenceKeyMigration {
    fun check(context: Context) {
        when (SafeSharedPreferences.version<PreferenceKey>(context)) {
            1 -> {
                migrateFromVersion1(context)
                migrateFromVersion2(context)
                migrateFromVersion3(context)
            }

            2 -> {
                migrateFromVersion2(context)
                migrateFromVersion3(context)
            }

            3 -> {
                migrateFromVersion3(context)
            }
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
                    putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, Category.MyBookmarks.id)
                    putInt(PreferenceKey.ENTRIES_INITIAL_TAB, 0)
                }

                // 2.
                Category.StarsReport -> {
                    putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, Category.Stars.id)
                }

                else -> { /* do nothing */ }
            }

            // 3.
            putInt(PreferenceKey.ENTRIES_INITIAL_TAB, initialTab % 2)
        }
    }

    /**
     * v3 -> v4
     *
     * バージョン引継ぎをした場合、「複数回タップ」の初期設定を「何もしない」にする
     */
    private fun migrateFromVersion3(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        prefs.edit {
            putInt(PreferenceKey.ENTRY_MULTIPLE_TAP_ACTION, TapEntryAction.NOTHING.id)
        }
    }
}

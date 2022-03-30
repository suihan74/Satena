package com.suihan74.satena.models

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import com.suihan74.satena.models.browser.ClearingImageCacheSpan
import com.suihan74.satena.models.browser.ReadEntryLifetime
import com.suihan74.satena.models.readEntry.ReadEntryBehavior
import com.suihan74.satena.models.readEntry.ReadEntryCondition
import com.suihan74.satena.scenes.bookmarks.BookmarksTabType
import com.suihan74.satena.scenes.bookmarks.TapTitleBarAction
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.entries2.CategoriesMode
import com.suihan74.satena.scenes.entries2.EntriesDefaultTabSettings
import com.suihan74.satena.scenes.entries2.ExtraBottomItemsAlignment
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.satena.scenes.post.TagsListOrder
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZonedDateTime

@SharedPreferencesKey(fileName = "default", version = 11, latest = true)
enum class PreferenceKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    ////////////////////////////////////////
    // accounts
    ////////////////////////////////////////

    /** 端末ごとのユニークID */
    ID(typeInfo<String>(), ""),

    /** はてなのログインID */
    @Deprecated("use only RK")
    HATENA_USER_NAME(typeInfo<String>(), ""),

    /** はてなのパスワード */
    @Deprecated("use only RK")
    HATENA_PASSWORD(typeInfo<String>(), ""),

    /** はてなのクッキー */
    HATENA_RK(typeInfo<String>(), ""),

    /** ID/Passwordを保存してクッキー失効時に自動的に再ログインする */
    @Deprecated("use only RK")
    SAVE_HATENA_USER_ID_PASSWORD(typeInfo<Boolean>(), false),

    /** Mastodonのアクセストークン(暗号化) */
    MASTODON_ACCESS_TOKEN(typeInfo<String>(), ""),

    /** Mastodon投稿時の公開範囲 */
    MASTODON_POST_VISIBILITY(typeInfo<Int>(), TootVisibility.PUBLIC.ordinal),

    ////////////////////////////////////////
    // generals
    ////////////////////////////////////////

    /** ダークテーマを使用 */
    @Deprecated("migrate to `THEME`")
    DARK_THEME(typeInfo<Boolean>(), false),

    /** アプリテーマ */
    THEME(typeInfo<Int>(), Theme.LIGHT.id),

    /** ダイアログのテーマ設定 */
    DIALOG_THEME(typeInfo<Int>(), DialogThemeSetting.APP.id),

    /** ダイアログの外側をタッチしたら閉じる */
    CLOSE_DIALOG_ON_TOUCH_OUTSIDE(typeInfo<Boolean>(), true),

    /** ドロワーの位置 */
    DRAWER_GRAVITY(typeInfo<Int>(), GravitySetting.END.gravity),

    /** 外部アプリでインテントを開く際にデフォルトアプリを優先して使用する */
    USE_INTENT_CHOOSER(typeInfo<Boolean>(), true),

    /** アプリ内アップデート通知 */
    APP_UPDATE_NOTICE_MODE(typeInfo<Int>(), AppUpdateNoticeMode.FIX.id),

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
    BACKGROUND_CHECKING_NOTICES_INTERVALS(typeInfo<Long>(), 15L),

    /** 最後に通知を確認した時刻 */
    NOTICES_LAST_SEEN(typeInfo<LocalDateTime>(), null),

    /** スパムと思われる通知を報せない */
    IGNORE_NOTICES_FROM_SPAM(typeInfo<Boolean>(), true),

    /** 最後に起動したときのアプリバージョン */
    APP_VERSION_LAST_LAUNCH(typeInfo<String>(), "0"),

    /** アップデート後最初の起動時にリリースノートを表示する */
    SHOW_RELEASE_NOTES_AFTER_UPDATE(typeInfo<Boolean>(), true),

    /** 画像キャッシュをクリアする間隔（日数） */
    CLEARING_IMAGE_CACHE_SPAN(typeInfo<Int>(), ClearingImageCacheSpan.MONTH_1.days),

    /** 最後に画像キャッシュをクリアした日時 */
    IMAGE_CACHE_LAST_CLEARED(typeInfo<ZonedDateTime>(), null),

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
    ENTRIES_BOTTOM_ITEMS_GRAVITY(typeInfo<Int>(), GravitySetting.END.gravity),

    /** ボトムバーの追加項目の表示位置 */
    ENTRIES_EXTRA_BOTTOM_ITEMS_ALIGNMENT(typeInfo<Int>(), ExtraBottomItemsAlignment.DEFAULT.id),

    /** エクストラスクロール機能のツマミの配置 */
    ENTRIES_EXTRA_SCROLL_ALIGNMENT(typeInfo<Int>(), ExtraScrollingAlignment.RIGHT.id),

    /** エントリ項目シングルタップの挙動 */
    ENTRY_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_COMMENTS.id),

    /** エントリ項目複数回タップの挙動 */
    ENTRY_MULTIPLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.id),

    /** エントリ項目ロングタップの挙動 */
    ENTRY_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.id),

    /** エントリ項目右端シングルタップの挙動 */
    ENTRY_EDGE_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_COMMENTS.id),

    /** エントリ項目右端複数回タップの挙動 */
    ENTRY_EDGE_MULTIPLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.id),

    /** エントリ項目右端ロングタップの挙動 */
    ENTRY_EDGE_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.id),

    /** タップ回数判定時間 0L~500L */
    ENTRY_MULTIPLE_TAP_DURATION(typeInfo<Long>(), 250L),

    /** 最初に表示するカテゴリ */
    ENTRIES_HOME_CATEGORY(typeInfo<Int>(), Category.All.id),

    /** 最初に表示するタブ(の位置) */
    @Deprecated("migrate to ENTRIES_DEFAULT_TABS")
    ENTRIES_INITIAL_TAB(typeInfo<Int>(), 0),

    /** 各カテゴリで最初に表示するタブ(の位置) */
    ENTRIES_DEFAULT_TABS(typeInfo<EntriesDefaultTabSettings>(), EntriesDefaultTabSettings().apply {
        put(Category.Memorial15th, 0)
    }),

    /** カテゴリリストの表示形式 */
    ENTRIES_CATEGORIES_MODE(typeInfo<Int>(), CategoriesMode.LIST.ordinal),

    /** メニュー展開時にタップ防止用の画面暗転をする */
    ENTRIES_MENU_TAP_GUARD(typeInfo<Boolean>(), true),

    /** コンテンツスクロールにあわせてツールバーを隠す */
    ENTRIES_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), true),

    /** タブ部分をロングタップしてホームカテゴリ・ホームタブを変更できるようにする */
    ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB(typeInfo<Boolean>(), true),

    /** タブのスワイプ感度 */
    ENTRIES_PAGER_SCROLL_SENSITIVITY(typeInfo<Float>(), 1f),

    /** エントリメニューから「あとで読む」ときデフォルトで非公開ブクマにする */
    ENTRY_PRIVATE_READ_LATER(typeInfo<Boolean>(), false),

    /** 「あとで読む」エントリを「読んだ」したときの挙動 */
    ENTRY_READ_ACTION_TYPE(typeInfo<Int>(), EntryReadActionType.SILENT_BOOKMARK.ordinal),

    /** EntryReadActionType.BOILERPLATE時の定型文 */
    ENTRY_READ_ACTION_BOILERPLATE(typeInfo<String>(), ""),

    /** エントリ検索設定 */
    ENTRY_SEARCH_SETTING(typeInfo<EntrySearchSetting>(), EntrySearchSetting()),

    /** 既読マークを表示するかどうか */
    @Deprecated("migrated to ENTRY_READ_BEHAVIOR")
    ENTRY_DISPLAY_READ_MARK(typeInfo<Boolean>(), true),

    /** 既読マークを表示するかどうか */
    ENTRY_READ_BEHAVIOR(typeInfo<Int>(), ReadEntryBehavior.DISPLAY_READ_MARK.int),

    /** どうしたら既読マークがつくか */
    ENTRY_READ_MARK_CONDITION(typeInfo<Int>(), ReadEntryCondition.BOOKMARKS_OR_PAGE_SHOWN.int),

    /** 既読マーク情報の寿命（日） */
    ENTRY_READ_MARK_LIFETIME(typeInfo<Int>(), ReadEntryLifetime.MONTH_3.days),

    /** 既読エントリを非表示にしないカテゴリ(PreferenceKey.ENTRY_READ_BEHAVIOR -> ReadEntryBehavior.HIDE_ENTRYのとき) */
    ENTRY_CATEGORIES_WITHOUT_HIDING_READ_ENTRY(typeInfo<List<Int>>(), Category.values().filterNot {
        it == Category.MyBookmarks || it == Category.History || it == Category.Stars
    }.map { it.id }),

    ////////////////////////////////////////
    // bookmarks
    ////////////////////////////////////////

    /** ブクマ登録前に確認ダイアログを表示する */
    USING_POST_BOOKMARK_DIALOG(typeInfo<Boolean>(), true),

    /** 最後に開いた投稿ダイアログでの各種状態を引き継ぐ */
    POST_BOOKMARK_SAVE_STATES(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのTwitter連携状態 */
    POST_BOOKMARK_TWITTER_LAST_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのMastodon連携状態 */
    POST_BOOKMARK_MASTODON_LAST_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのFacebook連携状態 */
    POST_BOOKMARK_FACEBOOK_LAST_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのプライベート選択状態 */
    POST_BOOKMARK_PRIVATE_LAST_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのTwitter連携状態 */
    POST_BOOKMARK_TWITTER_DEFAULT_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのMastodon連携状態 */
    POST_BOOKMARK_MASTODON_DEFAULT_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのFacebook連携状態 */
    POST_BOOKMARK_FACEBOOK_DEFAULT_CHECKED(typeInfo<Boolean>(), false),

    /** 最後に開いた投稿ダイアログでのプライベート選択状態 */
    POST_BOOKMARK_PRIVATE_DEFAULT_CHECKED(typeInfo<Boolean>(), false),

    /** 投稿ダイアログの表示位置(Y) */
    POST_BOOKMARK_VERTICAL_GRAVITY(typeInfo<Int>(), BookmarkPostActivityGravity.DEFAULT.ordinal),

    /** タグ入力ダイアログを最初から最大展開する */
    POST_BOOKMARK_EXPAND_ADDING_TAGS_DIALOG_BY_DEFAULT(typeInfo<Boolean>(), false),

    /** 投稿ダイアログのタグリストの並び順 */
    POST_BOOKMARK_TAGS_LIST_ORDER(typeInfo<Int>(), TagsListOrder.INDEX.ordinal),

    /** 過去の使用回数がn件以下のタグを表示しない(0=無効) */
//    POST_BOOKMARK_TAGS_LIST_LIMIT(typeInfo<Int>(), 0),

    /** 最初に表示するタブ */
    BOOKMARKS_INITIAL_TAB(typeInfo<Int>(), BookmarksTabType.POPULAR.ordinal),

    /** リストスクロールにあわせてメニューボタンを隠す */
    BOOKMARKS_HIDING_BUTTONS_BY_SCROLLING(typeInfo<Boolean>(), true),

    /** 「注目」タブで非表示対象も表示する */
    BOOKMARKS_SHOWING_IGNORED_BOOKMARKS_IN_DIGEST(typeInfo<Boolean>(), false),

    /** 「すべて」タブで非表示対象も表示する */
    BOOKMARKS_SHOWING_IGNORED_USERS_IN_ALL_BOOKMARKS(typeInfo<Boolean>(), true),

    /** IDコール先の非表示ユーザーのブクマを表示する */
    BOOKMARKS_SHOWING_IGNORED_USERS_WITH_CALLING(typeInfo<Boolean>(), false),

    /** ブクマ詳細画面で非表示ユーザーのスターをリストに表示する */
    BOOKMARKS_SHOWING_STARS_OF_IGNORED_USERS(typeInfo<Boolean>(), true),

    /** リストスクロールにあわせてツールバーを隠す */
    BOOKMARKS_HIDING_TOOLBAR_BY_SCROLLING(typeInfo<Boolean>(), false),

    /** タブのスワイプ感度 */
    BOOKMARKS_PAGER_SCROLL_SENSITIVITY(typeInfo<Float>(), 1f),

    /** エクストラスクロール機能のツマミの配置 */
    BOOKMARKS_EXTRA_SCROLL_ALIGNMENT(typeInfo<Int>(), ExtraScrollingAlignment.RIGHT.id),

    /** ブクマ一覧画面のリスト項目から直接スターを付けられるようにする */
    BOOKMARKS_USE_ADD_STAR_POPUP_MENU(typeInfo<Boolean>(), true),

    /** スター付与ボタンのタップ判定領域をブクマ項目右端部分に拡大する */
    BOOKMARKS_USE_ADD_STAR_EDGE(typeInfo<Boolean>(), true),

    /** スターを付ける前に確認ダイアログを表示する */
    USING_POST_STAR_DIALOG(typeInfo<Boolean>(), true),

    /** ユーザーを非表示にする前に確認ダイアログを表示する */
    USING_IGNORE_USER_DIALOG(typeInfo<Boolean>(), true),

    /** ブコメ中のリンクをシングルタップしたときの挙動 */
    BOOKMARK_LINK_SINGLE_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_PAGE.id),

    /** ブコメ中のリンクをロングタップしたときの挙動 */
    BOOKMARK_LINK_LONG_TAP_ACTION(typeInfo<Int>(), TapEntryAction.SHOW_MENU.id),

    /** タブ部分をロングタップして最初に表示するタブを変更できるようにする */
    BOOKMARKS_CHANGE_HOME_BY_LONG_TAPPING_TAB(typeInfo<Boolean>(), true),

    /** タイトル部分クリック時の挙動 */
    BOOKMARKS_TITLE_SINGLE_CLICK_BEHAVIOR(typeInfo<Int>(), TapTitleBarAction.SHOW_PAGE_WITH_DIALOG.id),

    /** タイトル部分ロングクリック時の挙動 */
    BOOKMARKS_TITLE_LONG_CLICK_BEHAVIOR(typeInfo<Int>(), TapTitleBarAction.SHOW_MENU.id),

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

    ;

    // ------- //

    companion object {
        /** バックグラウンド通知確認間隔の最小値 */
        const val BACKGROUND_CHECKING_NOTICES_INTERVALS_LOWER_BOUND = 15L
        /** バックグラウンド通知確認間隔の最大値 */
        const val BACKGROUND_CHECKING_NOTICES_INTERVALS_UPPER_BOUND = 180L
    }
}

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

@Suppress("deprecation")
object PreferenceKeyMigration {
    fun check(context: Context) {
        while (true) {
            when (SafeSharedPreferences.version<PreferenceKey>(context)) {
                1 -> migrateFromVersion1(context)

                2 -> migrateFromVersion2(context)

                3 -> migrateFromVersion3(context)

                4 -> migrateFromVersion4(context)

                5 -> migrateFromVersion5(context)

                6 -> migrateFromVersion6(context)

                7 -> migrateFromVersion7(context)

                8 -> migrateFromVersion8(context)

                9 -> migrateFromVersion9(context)

                else -> break
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
        val homeCategory = Category.fromId(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
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

    /**
     * v4 -> v5
     *
     * 新着通知確認の常駐方法を変更した影響で
     * 通知確認間隔の最小値を15分に制限した
     */
    private fun migrateFromVersion4(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.BACKGROUND_CHECKING_NOTICES_INTERVALS

        prefs.edit {
            val result = runCatching {
                if (prefs.getLong(key) < 15L) {
                    putLong(key, 15L)
                }
            }

            if (result.isFailure) {
                putLong(key, 15L)
            }
        }
    }

    /**
     * v5 -> v6
     *
     * テーマの保存方法を変更
     */
    private fun migrateFromVersion5(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.DARK_THEME

        prefs.edit {
            val result = runCatching {
                if (prefs.getBoolean(key)) {
                    put(PreferenceKey.THEME, Theme.DARK.id)
                }
                else {
                    put(PreferenceKey.THEME, Theme.LIGHT.id)
                }
                remove(PreferenceKey.DARK_THEME)
            }

            if (result.isFailure) {
                runCatching {
                    remove(PreferenceKey.DARK_THEME)
                    remove(PreferenceKey.THEME)
                }
            }
        }
    }

    /**
     * v6 -> v7
     *
     * `Gravity`を`GravitySetting`に変更
     *
     * それに伴い`Gravity.LEFT`,`Gravity.RIGHT`を`Gravity.START`,`Gravity.END`に修正する
     */
    @SuppressLint("RtlHardcoded")
    private fun migrateFromVersion6(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val key = PreferenceKey.DRAWER_GRAVITY

        prefs.edit {
            val result = runCatching {
                if (Gravity.LEFT == prefs.getInt(key)) {
                    put(key, GravitySetting.START.gravity)
                }
                else {
                    put(key, GravitySetting.END.gravity)
                }
            }

            if (result.isFailure) {
                runCatching {
                    remove(PreferenceKey.DRAWER_GRAVITY)
                }
            }
        }
    }

    /**
     * v7 -> v8
     *
     * `ENTRIES_INITIAL_TAB`を`ENTRIES_DEFAULT_TABS`に移行
     */
    private fun migrateFromVersion7(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        runCatching {
            val oldKey = PreferenceKey.ENTRIES_INITIAL_TAB
            val newKey = PreferenceKey.ENTRIES_DEFAULT_TABS

            val homeCategory = Category.fromId(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
            val initialTab = prefs.getInt(oldKey)
            val defaultTabs =
                prefs.getObject<EntriesDefaultTabSettings>(newKey)!!
            defaultTabs[homeCategory] = initialTab

            prefs.edit {
                putObject(newKey, defaultTabs)
                remove(oldKey)
            }
        }.onFailure {
            prefs.edit {}
        }
    }

    /**
     * v8 -> v9
     *
     * 鯖側の仕様変更に追従する際アプリ側で不要になったHatenaのID/パスワードを保存しないようにした
     */
    private fun migrateFromVersion8(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        runCatching {
            prefs.edit {
                remove(PreferenceKey.HATENA_USER_NAME)
                remove(PreferenceKey.HATENA_PASSWORD)
                remove(PreferenceKey.SAVE_HATENA_USER_ID_PASSWORD)
            }
        }.onFailure {
            prefs.edit {}
        }
    }

    /**
     * v9 -> v10
     *
     * エントリのタップアクショントリガを追加。既存ユーザーは挙動がこれまでと変わらないように初期設定する
     */
    private fun migrateFromVersion9(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        runCatching {
            prefs.edit {
                putInt(PreferenceKey.ENTRY_EDGE_SINGLE_TAP_ACTION, prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))
                putInt(PreferenceKey.ENTRY_EDGE_LONG_TAP_ACTION, prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))
                putInt(PreferenceKey.ENTRY_EDGE_MULTIPLE_TAP_ACTION, prefs.getInt(PreferenceKey.ENTRY_MULTIPLE_TAP_ACTION))
            }
        }.onFailure {
            prefs.edit {}
        }
    }

    /**
     * v10 -> v11
     *
     * 「既読マークの表示/非表示」設定を「既読エントリの振舞い」に変更
     */
    private fun migrateFromVersion10(context: Context) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        runCatching {
            prefs.edit {
                val readMarkVisible = prefs.getBoolean(PreferenceKey.ENTRY_DISPLAY_READ_MARK)
                val behavior =
                    if (readMarkVisible) ReadEntryBehavior.DISPLAY_READ_MARK
                    else ReadEntryBehavior.NONE
                putInt(PreferenceKey.ENTRY_READ_BEHAVIOR, behavior.int)
                remove(PreferenceKey.ENTRY_DISPLAY_READ_MARK)
            }
        }.onFailure {
            prefs.edit {}
        }
    }
}

package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.entries2.AdditionalBottomItemsAlignment
import com.suihan74.satena.scenes.entries2.CategoriesMode
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.satena.scenes.preferences.bottomBar.BottomBarItemSelectionDialog
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesEntriesViewModel(
    prefs: SafeSharedPreferences<PreferenceKey>,
    historyPrefs: SafeSharedPreferences<EntriesHistoryKey>
) : PreferencesViewModel(prefs), BottomBarItemSelectionDialog.Listener {

    /** レイアウトモード */
    val bottomLayoutMode = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_BOTTOM_LAYOUT_MODE
    )

    /** 下部レイアウトをスクロールで隠す */
    val hideBottomLayoutByScroll = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_HIDE_BOTTOM_LAYOUT_BY_SCROLLING
    )

    /** 下部バーに表示するボタン */
    val bottomBarButtons = createLiveData<List<UserBottomItem>>(
        PreferenceKey.ENTRIES_BOTTOM_ITEMS
    )

    /** 下部バーの項目を右詰めで表示するか左詰めで表示するか */
    val bottomBarButtonsGravity = createLiveData<Int>(
        PreferenceKey.ENTRIES_BOTTOM_ITEMS_GRAVITY
    )

    /** 下部バーの追加項目の配置方法 */
    val additionalBottomItemsAlignment = createLiveDataEnum(
        PreferenceKey.ENTRIES_ADDITIONAL_BOTTOM_ITEMS_ALIGNMENT,
        { it.id },
        { AdditionalBottomItemsAlignment.fromInt(it) }
    )

    /** エントリ項目シングルタップの挙動 */
    val singleTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_SINGLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromInt(it) }
    )

    /** エントリ項目複数回タップの挙動 */
    val multipleTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_MULTIPLE_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromInt(it) }
    )

    /** エントリ項目ロングタップの挙動 */
    val longTapAction = createLiveDataEnum(
        PreferenceKey.ENTRY_LONG_TAP_ACTION,
        { it.id },
        { TapEntryAction.fromInt(it) }
    )

    /** 最初に表示するカテゴリ */
    val homeCategory = createLiveDataEnum<Category>(
        PreferenceKey.ENTRIES_HOME_CATEGORY,
        { it.id },
        { Category.fromInt(it) }
    )

    /** 最初に表示するタブ */
    val initialTab = createLiveData<Int>(
        PreferenceKey.ENTRIES_INITIAL_TAB
    )

    /** カテゴリリストの表示形式 */
    val categoriesMode = createLiveDataEnum<CategoriesMode>(
        PreferenceKey.ENTRIES_CATEGORIES_MODE
    )

    /** メニュー表示中の操作を許可 */
    val menuTapGuard = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_MENU_TAP_GUARD
    )

    /** スクロールでツールバーを隠す */
    val hideToolbarWithScroll = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING
    )

    /** ブクマ閲覧履歴の最大保存数 */
    val historyMaxSize = createLiveData<EntriesHistoryKey, Int>(
        historyPrefs,
        EntriesHistoryKey.MAX_SIZE
    )

    /** タブ長押しでホームカテゴリ・初期タブを変更する */
    val changeHomeByLongTappingTab = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_CHANGE_HOME_BY_LONG_TAPPING_TAB
    )

    /** 「あとで読む」エントリを「読んだ」したときの挙動 */
    val entryReadActionType = createLiveDataEnum<EntryReadActionType>(
        PreferenceKey.ENTRY_READ_ACTION_TYPE
    )

    /** EntryReadActionType.BOILERPLATE時の定型文 */
    val entryReadActionBoilerPlate = createLiveData<String>(
        PreferenceKey.ENTRY_READ_ACTION_BOILERPLATE
    )

    /** ボトムバーの項目を追加・編集 */
    override fun onSelectItem(position: Int, old: UserBottomItem?, new: UserBottomItem?) {
        if (old == null && new == null) return
        else if (new == null) {
            bottomBarButtons.value = bottomBarButtons.value?.minus(old!!)
        }
        else if (old == null) {
            bottomBarButtons.value = (bottomBarButtons.value ?: emptyList()).plus(new)
        }
        else {
            bottomBarButtons.value = bottomBarButtons.value?.mapIndexed { idx, item ->
                if (idx == position) new
                else item
            }
        }
    }

    /** ボトムバーの項目を入れ替え */
    override fun onReorderItem(
        positionA: Int,
        positionB: Int,
        itemA: UserBottomItem?,
        itemB: UserBottomItem
    ) {
        val items = bottomBarButtons.value ?: emptyList()
        if (itemA == null) {
            // 末尾に移動
            bottomBarButtons.value = items.minus(itemB).plus(itemB)
        }
        else {
            // 項目同士の入れ替え
            bottomBarButtons.value = items.mapIndexed { idx, item ->
                when (idx) {
                    positionA -> itemB
                    positionB -> itemA
                    else -> item
                }
            }
        }
    }

    class Factory(
        private val prefs: SafeSharedPreferences<PreferenceKey>,
        private val historyPrefs: SafeSharedPreferences<EntriesHistoryKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            PreferencesEntriesViewModel(prefs, historyPrefs) as T
    }
}

package com.suihan74.satena.scenes.preferences.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

class PreferencesEntriesViewModel(
    prefs: SafeSharedPreferences<PreferenceKey>,
    historyPrefs: SafeSharedPreferences<EntriesHistoryKey>
) : PreferencesViewModel(prefs) {

    /** レイアウトモード */
    val bottomLayoutMode = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_BOTTOM_LAYOUT_MODE
    )

    /** 下部レイアウトをスクロールで隠す */
    val hideBottomLayoutByScroll = createLiveData<Boolean>(
        PreferenceKey.ENTRIES_HIDE_BOTTOM_LAYOUT_BY_SCROLLING
    )

    /** エントリ項目シングルタップの挙動 */
    val singleTapAction = createLiveDataEnum<TapEntryAction>(
        PreferenceKey.ENTRY_SINGLE_TAP_ACTION
    )

    /** エントリ項目ロングタップの挙動 */
    val longTapAction = createLiveDataEnum<TapEntryAction>(
        PreferenceKey.ENTRY_LONG_TAP_ACTION
    )

    /** 最初に表示するカテゴリ */
    val homeCategory = createLiveDataEnum<Category>(
        PreferenceKey.ENTRIES_HOME_CATEGORY
    )

    /** 最初に表示するタブ */
    val initialTab = createLiveData<Int>(
        PreferenceKey.ENTRIES_INITIAL_TAB
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

    class Factory(
        private val prefs: SafeSharedPreferences<PreferenceKey>,
        private val historyPrefs: SafeSharedPreferences<EntriesHistoryKey>
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            PreferencesEntriesViewModel(prefs, historyPrefs) as T
    }
}

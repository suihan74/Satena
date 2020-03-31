package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.EntriesHistoryKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.toVisibility
import kotlinx.android.synthetic.main.fragment_preferences_entries.view.*

class PreferencesEntriesFragment :
    PreferencesFragmentBase(),
    AlertDialogFragment.Listener,
    NumberPickerDialogFragment.Listener
{
    companion object {
        fun createInstance() =
            PreferencesEntriesFragment()

        private const val DIALOG_SINGLE_TAP_ACTION = "DIALOG_SINGLE_TAP_ACTION"
        private const val DIALOG_LONG_TAP_ACTION = "DIALOG_LONG_TAP_ACTION"
        private const val DIALOG_HOME_CATEGORY = "DIALOG_HOME_CATEGORY"
        private const val DIALOG_HOME_TAB = "DIALOG_HOME_TAB"
        private const val DIALOG_HISTORY_MAX_SIZE_PICKER = "DIALOG_HISTORY_MAX_SIZE_PICKER"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_entries, container, false)

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val tapActions = TapEntryAction.values().map { getString(it.titleId) }.toTypedArray()

        // シングルタップ時の動作
        view.preferences_entries_single_tap_action.apply {
            setText(TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION)).titleId)
            setOnClickListener {
                val currentAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_single_tap_action_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(tapActions, currentAction.ordinal)
                    .show(childFragmentManager, DIALOG_SINGLE_TAP_ACTION)
            }
        }

        // ロングタップ時の動作
        view.preferences_entries_long_tap_action.apply {
            setText(TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION)).titleId)
            setOnClickListener {
                val currentAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_long_tap_action_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(tapActions, currentAction.ordinal)
                    .show(childFragmentManager, DIALOG_LONG_TAP_ACTION)
            }
        }

        // ホームカテゴリ
        val initialHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        view.preferences_home_category.apply {
            setText(initialHomeCategory.textId)
            setOnClickListener {
                val currentHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))

                val categories =
                    if (HatenaClient.signedIn()) Category.valuesWithSignedIn()
                    else Category.valuesWithoutSignedIn()

                val currentOrdinal = categories.indexOf(currentHomeCategory)

                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_home_category_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setAdditionalData("categories", categories)
                    .setSingleChoiceItems(
                        categories.map { getString(it.textId) },
                        currentOrdinal)
                    .show(childFragmentManager, DIALOG_HOME_CATEGORY)
            }
        }

        // 最初に表示するタブ
        val initialTabItemVisibility = (!initialHomeCategory.singleColumns).toVisibility()
        view.preferences_entries_initial_tab_desc.visibility = initialTabItemVisibility
        view.preferences_entries_initial_tab.apply {
            val key = PreferenceKey.ENTRIES_INITIAL_TAB

            val initialTabs = getTabTitleIds(prefs)
            val tabIdx = prefs.getInt(key)
            setText(initialTabs[tabIdx])
            visibility = initialTabItemVisibility

            setOnClickListener {
                val items = getTabTitleIds(prefs)

                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_initial_tab_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(
                        items.map { context.getString(it) },
                        prefs.getInt(key))
                    .show(childFragmentManager, DIALOG_HOME_TAB)
            }
        }

        // メニュー表示中の操作を許可
        view.preferences_entries_menu_tap_guard.apply {
            isChecked = prefs.getBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD, isChecked)
                }
            }
        }

        // スクロールでツールバーを隠す
        view.preferences_entries_hiding_toolbar_by_scrolling.apply {
            val key = PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // ブクマ閲覧履歴の最大保存数
        view.preferences_entries_history_max_size.apply {
            val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
            val key = EntriesHistoryKey.MAX_SIZE
            text = getString(R.string.pref_entries_history_max_size_text, historyPrefs.getInt(key))

            setOnClickListener {
                val currentMaxSize = historyPrefs.getInt(key)

                NumberPickerDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_history_max_size_dialog_title)
                    .setMessage(R.string.pref_entries_history_max_size_dialog_msg)
                    .setMinValue(1)
                    .setMaxValue(100)
                    .setDefaultValue(currentMaxSize)
                    .show(childFragmentManager, DIALOG_HISTORY_MAX_SIZE_PICKER)
            }
        }

        return view
    }

    private fun getTabTitleIds(prefs: SafeSharedPreferences<PreferenceKey>) : List<Int> =
        when (Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))) {
            Category.MyBookmarks -> listOf(R.string.entries_tab_mybookmarks, R.string.entries_tab_read_later)
            Category.Stars -> listOf(R.string.entries_tab_my_stars, R.string.entries_tab_stars_report)
            else -> listOf(R.string.entries_tab_hot, R.string.entries_tab_recent)
        }

    /** ダイアログ項目の選択 */
    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        when (dialog.tag) {
            DIALOG_SINGLE_TAP_ACTION -> {
                val act = TapEntryAction.fromInt(which)
                prefs.edit {
                    putInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION, act.ordinal)
                }
                view?.preferences_entries_single_tap_action?.run {
                    text = dialog.items!![which]
                }
                dialog.dismiss()
            }

            DIALOG_LONG_TAP_ACTION -> {
                val act = TapEntryAction.fromInt(which)
                prefs.edit {
                    putInt(PreferenceKey.ENTRY_LONG_TAP_ACTION, act.ordinal)
                }
                view?.preferences_entries_long_tap_action?.run {
                    text = dialog.items!![which]
                }
                dialog.dismiss()
            }

            DIALOG_HOME_CATEGORY -> {
                val categories = dialog.getAdditionalData<Array<Category>>("categories")!!
                val cat = categories[which]
                prefs.edit {
                    putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, cat.ordinal)
                }
                view?.preferences_home_category?.run {
                    text = dialog.items!![which]
                }

                view?.run {
                    val v = (!cat.singleColumns).toVisibility()
                    preferences_entries_initial_tab_desc?.visibility = v
                    preferences_entries_initial_tab?.run initialTab@ {
                        val tabIdx = prefs.getInt(PreferenceKey.ENTRIES_INITIAL_TAB)
                        val tabTitles = getTabTitleIds(prefs)
                        setText(tabTitles[tabIdx])
                        visibility = v
                    }
                }
                dialog.dismiss()
            }

            DIALOG_HOME_TAB -> {
                prefs.edit {
                    putInt(PreferenceKey.ENTRIES_INITIAL_TAB, which)
                }

                view?.preferences_entries_initial_tab?.run {
                    text = dialog.items!![which]
                }

                dialog.dismiss()
            }
        }
    }

    /** NumberPickerの処理完了 */
    override fun onCompleteNumberPicker(value: Int, dialog: NumberPickerDialogFragment) {
        val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
        historyPrefs.edit {
            putInt(EntriesHistoryKey.MAX_SIZE, value)
        }
        view?.preferences_entries_history_max_size?.run {
            text = getString(R.string.pref_entries_history_max_size_text, value)
        }
    }
}

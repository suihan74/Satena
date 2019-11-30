package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.AlertDialogListener
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.toVisibility

class PreferencesEntriesFragment :
    PreferencesFragmentBase(),
    AlertDialogListener,
    NumberPickerDialogFragment.Listener
{
    companion object {
        fun createInstance() =
            PreferencesEntriesFragment()
    }

    private var mRoot: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_entries, container, false)
        mRoot = view

        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val tapActions = TapEntryAction.values().map { getString(it.titleId) }.toTypedArray()

        // シングルタップ時の動作
        view.findViewById<Button>(R.id.preferences_entries_single_tap_action).apply {
            text = getText(TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION)).titleId)
            setOnClickListener {
                val currentAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_single_tap_action_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(tapActions, currentAction.ordinal)
                    .show(childFragmentManager, "single_tap_action_dialog")
            }
        }

        // ロングタップ時の動作
        view.findViewById<Button>(R.id.preferences_entries_long_tap_action).apply {
            text = getText(TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION)).titleId)
            setOnClickListener {
                val currentAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_long_tap_action_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(tapActions, currentAction.ordinal)
                    .show(childFragmentManager, "long_tap_action_dialog")
            }
        }

        // ホームカテゴリ
        val initialHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        view.findViewById<Button>(R.id.preferences_home_category).apply {
            text = getCategoryName(initialHomeCategory)
            setOnClickListener {
                val currentHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))

                val categories =
                    if (HatenaClient.signedIn())
                        Category.valuesWithSignedIn()
                    else
                        Category.valuesWithoutSignedIn()

                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_home_category_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(
                        categories.map { getCategoryName(it) },
                        currentHomeCategory.ordinal)
                    .show(childFragmentManager, "home_category_dialog")
            }
        }

        // 最初に表示するタブ
        val initialTabItemVisibility = (!initialHomeCategory.singleColumns).toVisibility()
        view.findViewById<View>(R.id.preferences_entries_initial_tab_desc).visibility = initialTabItemVisibility
        view.findViewById<Button>(R.id.preferences_entries_initial_tab).apply {
            val key = PreferenceKey.ENTRIES_INITIAL_TAB
            val tabOffset = if (initialHomeCategory == Category.MyBookmarks) 2 else 0
            text = context.getText(EntriesTabType.fromInt(prefs.getInt(key) + tabOffset).textId)
            visibility = initialTabItemVisibility
            setOnClickListener {
                val currentInitialTab = EntriesTabType.fromInt(prefs.getInt(key) + tabOffset)
                val currentHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
                val items =
                    if (currentHomeCategory == Category.MyBookmarks) {
                        listOf(EntriesTabType.MYBOOKMARKS, EntriesTabType.READLATER)
                    }
                    else {
                        listOf(EntriesTabType.POPULAR, EntriesTabType.RECENT)
                    }

                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_initial_tab_desc)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setSingleChoiceItems(
                        items.map { context.getString(it.textId) },
                        currentInitialTab.ordinal - tabOffset)
                    .show(childFragmentManager, "home_tab_dialog")
            }
        }

        // メニュー表示中の操作を許可
        view.findViewById<ToggleButton>(R.id.preferences_entries_menu_tap_guard).apply {
            isChecked = prefs.getBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(PreferenceKey.ENTRIES_MENU_TAP_GUARD, isChecked)
                }
            }
        }

        // スクロールでツールバーを隠す
        view.findViewById<ToggleButton>(R.id.preferences_entries_hiding_toolbar_by_scrolling).apply {
            val key = PreferenceKey.ENTRIES_HIDING_TOOLBAR_BY_SCROLLING
            isChecked = prefs.getBoolean(key)
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit {
                    putBoolean(key, isChecked)
                }
            }
        }

        // ブクマ閲覧履歴の最大保存数
        view.findViewById<Button>(R.id.preferences_entries_history_max_size).apply {
            val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
            val key = EntriesHistoryKey.MAX_SIZE
            text = String.format("%d件", historyPrefs.getInt(key))

            setOnClickListener {
                val currentMaxSize = historyPrefs.getInt(key)

                NumberPickerDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(R.string.pref_entries_history_max_size_dialog_title)
                    .setMessage(R.string.pref_entries_history_max_size_dialog_msg)
                    .setMinValue(1)
                    .setMaxValue(100)
                    .setDefaultValue(currentMaxSize)
                    .show(childFragmentManager, "history_max_size_picker")
            }
        }

        return view
    }

    private fun getCategoryName(cat: Category) = getString(cat.textId)

    fun <T : View> findViewById(id: Int) =
        mRoot?.findViewById<T>(id)

    override fun onSingleSelectItem(dialog: AlertDialogFragment, which: Int) {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)

        when (dialog.tag) {
            "single_tap_action_dialog" -> {
                val act = TapEntryAction.fromInt(which)
                prefs.edit {
                    putInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION, act.ordinal)
                }
                view?.findViewById<Button>(R.id.preferences_entries_single_tap_action)?.run {
                    text = dialog.items!![which]
                }
                dialog.dismiss()
            }

            "long_tap_action_dialog" -> {
                val act = TapEntryAction.fromInt(which)
                prefs.edit {
                    putInt(PreferenceKey.ENTRY_LONG_TAP_ACTION, act.ordinal)
                }
                view?.findViewById<Button>(R.id.preferences_entries_long_tap_action)?.run {
                    text = dialog.items!![which]
                }
                dialog.dismiss()
            }

            "home_category_dialog" -> {
                val cat = Category.fromInt(which)
                prefs.edit {
                    putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, cat.ordinal)
                }
                view?.findViewById<Button>(R.id.preferences_home_category)?.run {
                    text = dialog.items!![which]
                }

                val v = (!cat.singleColumns).toVisibility()
                view?.findViewById<View>(R.id.preferences_entries_initial_tab_desc)?.visibility = v
                view?.findViewById<Button>(R.id.preferences_entries_initial_tab)?.run initialTab@ {
                    this@initialTab.visibility = v
                    val tabOffset = if (cat == Category.MyBookmarks) 2 else 0
                    val key = PreferenceKey.ENTRIES_INITIAL_TAB
                    val currentInitialTab = EntriesTabType.fromInt(prefs.getInt(key) + tabOffset)
                    this@initialTab.text = context.getText(currentInitialTab.textId)
                }

                dialog.dismiss()
            }

            "home_tab_dialog" -> {
                prefs.edit {
                    putInt(PreferenceKey.ENTRIES_INITIAL_TAB, which)
                }

                view?.findViewById<Button>(R.id.preferences_entries_initial_tab)?.run {
                    text = dialog.items!![which]
                }

                dialog.dismiss()
            }
        }
    }

    override fun onCompleteNumberPicker(value: Int, dialog: NumberPickerDialogFragment) {
        val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
        historyPrefs.edit {
            putInt(EntriesHistoryKey.MAX_SIZE, value)
        }
        view?.findViewById<Button>(R.id.preferences_entries_history_max_size)?.run {
            text = String.format("%d件", value)
        }
    }
}

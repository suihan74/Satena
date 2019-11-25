package com.suihan74.satena.scenes.preferences.pages

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.get
import com.suihan74.utilities.toVisibility

class PreferencesEntriesFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() =
            PreferencesEntriesFragment()
    }

    private var mRoot: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_entries, container, false)
        mRoot = view

        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)
        val tapActions = TapEntryAction.values().map { getString(it.titleId) }.toTypedArray()

        // シングルタップ時の動作
        var currentSingleTapEntryAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))
        view.findViewById<Button>(R.id.preferences_entries_single_tap_action).apply {
            text = getText(currentSingleTapEntryAction.titleId)
            setOnClickListener {
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getText(R.string.pref_entries_single_tap_action_desc))
                    .setSingleChoiceItems(tapActions, currentSingleTapEntryAction.ordinal) { dialog, which ->
                        val act = TapEntryAction.fromInt(which)
                        prefs.edit {
                            putInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION, act.ordinal)
                        }
                        currentSingleTapEntryAction = act
                        text = tapActions[which]
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
        }

        // ロングタップ時の動作
        var currentLongTapEntryAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_LONG_TAP_ACTION))
        view.findViewById<Button>(R.id.preferences_entries_long_tap_action).apply {
            text = getText(currentLongTapEntryAction.titleId)
            setOnClickListener {
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getText(R.string.pref_entries_long_tap_action_desc))
                    .setSingleChoiceItems(
                        tapActions,
                        currentLongTapEntryAction.ordinal
                    ) { dialog, which ->
                        val act = TapEntryAction.fromInt(which)
                        prefs.edit {
                            putInt(PreferenceKey.ENTRY_LONG_TAP_ACTION, act.ordinal)
                        }
                        currentLongTapEntryAction = act
                        text = tapActions[which]
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
        }

        // ホームカテゴリ
        var currentHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
        view.findViewById<Button>(R.id.preferences_home_category).apply {
            text = getCategoryName(currentHomeCategory)
            setOnClickListener {
                val categories =
                    if (HatenaClient.signedIn())
                        Category.valuesWithSignedIn()
                    else
                        Category.valuesWithoutSignedIn()

                val categoryTexts = categories.map { getCategoryName(it) }.toTypedArray()
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getText(R.string.pref_home_category_desc))
                    .setSingleChoiceItems(categoryTexts, currentHomeCategory.ordinal) { dialog, which ->
                        val cat = Category.fromInt(which)
                        prefs.edit {
                            putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, cat.ordinal)
                        }
                        text = categoryTexts[which]
                        currentHomeCategory = cat

                        val v = (!cat.singleColumns).toVisibility()
                        view.findViewById<View>(R.id.preferences_entries_initial_tab_desc).visibility = v
                        view.findViewById<Button>(R.id.preferences_entries_initial_tab).apply initialTab@ {
                            this@initialTab.visibility = v
                            val tabOffset = if (cat == Category.MyBookmarks) 2 else 0
                            val key = PreferenceKey.ENTRIES_INITIAL_TAB
                            val currentInitialTab = EntriesTabType.fromInt(prefs.getInt(key) + tabOffset)
                            this@initialTab.text = context.getText(currentInitialTab.textId)
                        }

                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
        }

        // 最初に表示するタブ
        val initialTabItemVisibility = (!currentHomeCategory.singleColumns).toVisibility()
        view.findViewById<View>(R.id.preferences_entries_initial_tab_desc).visibility = initialTabItemVisibility
        view.findViewById<Button>(R.id.preferences_entries_initial_tab).apply {
            val key = PreferenceKey.ENTRIES_INITIAL_TAB
            val tabOffset = if (currentHomeCategory == Category.MyBookmarks) 2 else 0
            var currentInitialTab = EntriesTabType.fromInt(prefs.getInt(key) + tabOffset)
            text = context.getText(currentInitialTab.textId)
            visibility = initialTabItemVisibility
            setOnClickListener {
                val items = if (currentHomeCategory == Category.MyBookmarks) {
                    listOf(EntriesTabType.MYBOOKMARKS, EntriesTabType.READLATER)
                }
                else {
                    listOf(EntriesTabType.POPULAR, EntriesTabType.RECENT)
                }

                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getText(R.string.pref_entries_initial_tab_desc))
                    .setSingleChoiceItems(
                        items
                            .map { context.getText(it.textId) }
                            .toTypedArray(),
                        currentInitialTab.ordinal - tabOffset
                    ) { dialog, which ->
                        val tab = items[which]
                        prefs.edit {
                            putInt(key, which)
                        }
                        currentInitialTab = tab
                        this.text = context.getText(tab.textId)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
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
            val key = EntriesHistoryKey.MAX_SIZE
            val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
            var currentMaxSize = historyPrefs.getInt(key)
            text = String.format("%d件", currentMaxSize)
            setOnClickListener {
                val dialog = NumberPickerDialogFragment.createInstance(
                    title = "ブクマ閲覧履歴の最大保存数を設定",
                    message = "1～100件で指定できます",
                    minValue = 1,
                    maxValue = 100,
                    defaultValue = currentMaxSize
                ) { fm, value ->
                    currentMaxSize = value
                    historyPrefs.edit {
                        putInt(key, value)
                    }

                    fm.get<PreferencesEntriesFragment>()?.findViewById<Button>(R.id.preferences_entries_history_max_size)?.let {
                        it.text = String.format("%d件", value)
                    }
                }
                dialog.show(fragmentManager!!, "dialog")
            }
        }

        return view
    }

    private fun getCategoryName(cat: Category) = getString(cat.textId)

    fun <T : View> findViewById(id: Int) =
        mRoot?.findViewById<T>(id)
}

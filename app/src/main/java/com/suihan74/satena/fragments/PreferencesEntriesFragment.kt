package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.suihan74.HatenaLib.Category
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.EntriesTabType
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TapEntryAction
import com.suihan74.utilities.SafeSharedPreferences
import java.util.*

class PreferencesEntriesFragment : Fragment() {
    companion object {
        fun createInstance() = PreferencesEntriesFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_preferences_entries, container, false)
        val prefs = SafeSharedPreferences.create<PreferenceKey>(activity)
        val tapActions = TapEntryAction.values().map { getString(it.titleId) }.toTypedArray()

        // シングルタップ時の動作
        var currentSingleTapEntryAction = TapEntryAction.fromInt(prefs.getInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION))
        view.findViewById<Button>(R.id.preferences_entries_single_tap_action).apply {
            text = getText(currentSingleTapEntryAction.titleId)
            setOnClickListener {
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getText(R.string.pref_entries_single_tap_action_desc))
                    .setSingleChoiceItems(tapActions, currentSingleTapEntryAction.int) { dialog, which ->
                        val act = TapEntryAction.fromInt(which)
                        prefs.edit {
                            putInt(PreferenceKey.ENTRY_SINGLE_TAP_ACTION, act.int)
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
                        currentLongTapEntryAction.int
                    ) { dialog, which ->
                        val act = TapEntryAction.fromInt(which)
                        prefs.edit {
                            putInt(PreferenceKey.ENTRY_LONG_TAP_ACTION, act.int)
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
        view.findViewById<Button>(R.id.preferences_home_category).apply {
            var currentHomeCategory = Category.fromInt(prefs.getInt(PreferenceKey.ENTRIES_HOME_CATEGORY))
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
                    .setSingleChoiceItems(categoryTexts, currentHomeCategory.int) { dialog, which ->
                        val cat = Category.fromInt(which)
                        prefs.edit {
                            putInt(PreferenceKey.ENTRIES_HOME_CATEGORY, cat.int)
                        }
                        text = categoryTexts[which]
                        currentHomeCategory = cat
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show()
            }
        }

        // 最初に表示するタブ
        view.findViewById<Button>(R.id.preferences_entries_initial_tab).apply {
            val key = PreferenceKey.ENTRIES_INITIAL_TAB
            var currentInitialTab = EntriesTabType.fromInt(prefs.getInt(key))
            text = context.getText(currentInitialTab.textId)
            setOnClickListener {
                AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                    .setTitle(getText(R.string.pref_entries_initial_tab_desc))
                    .setSingleChoiceItems(
                        listOf(EntriesTabType.POPULAR, EntriesTabType.RECENT)
                            .map { context.getText(it.textId) }
                            .toTypedArray(),
                        currentInitialTab.int
                    ) { dialog, which ->
                        val tab = EntriesTabType.fromInt(which)
                        prefs.edit {
                            putInt(key, tab.int)
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

        return view
    }

    private fun getCategoryName(cat: Category) : String {
        val id = resources.getIdentifier(
            "category_${cat.name.toLowerCase(Locale.ROOT)}",
            "string",
            context!!.packageName)
        return getString(id)
    }
}

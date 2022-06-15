package com.suihan74.satena.scenes.preferences.entries

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.Category
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.entries2.EntriesDefaultTabSettings
import com.suihan74.satena.scenes.entries2.EntriesTabType
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.alsoAs

class EntriesDefaultTabsViewModel(private val prefs : SafeSharedPreferences<PreferenceKey>) : ViewModel() {
    private val tabsMap = prefs.getObject<EntriesDefaultTabSettings>(PreferenceKey.ENTRIES_DEFAULT_TABS)!!

    val settings : List<EntriesDefaultTabSetting>

    // ------ //

    init {
        val categories = Category.values()
        settings = categories
            .filterNot { it.singleColumns || it == Category.Memorial15th }
            .filter {
                it.declaringClass.getField(it.name).getAnnotation(Deprecated::class.java) == null
            }
            .map {
                EntriesDefaultTabSetting(
                    category = it,
                    tab = MutableLiveData(
                        EntriesTabType.fromTabOrdinal(tabsMap.getOrDefault(it), it)
                    )
                )
            }
    }

    // ------ //

    fun openTabSelectionDialog(category: Category, fragmentManager: FragmentManager) {
        val items = buildList {
            add(EntriesTabType.MAINTAIN)
            addAll(EntriesTabType.getTabs(category))
        }
        val labels = items.map { it.textId }
        val selectedTabOrdinal = tabsMap.getOrDefault(category)
        val checkedItem = items.indexOfFirst { it.tabOrdinal == selectedTabOrdinal }

        AlertDialogFragment.Builder()
            .setTitle(category.textId)
            .setSingleChoiceItems(labels, checkedItem) { _, which ->
                val tabOrdinal = items[which].tabOrdinal
                settings.first { it.category == category }
                    .tab.alsoAs<MutableLiveData<EntriesTabType>> { liveData ->
                        liveData.value = EntriesTabType.fromTabOrdinal(tabOrdinal, category)
                    }
                tabsMap[category] = tabOrdinal
                prefs.edit {
                    putObject(PreferenceKey.ENTRIES_DEFAULT_TABS, tabsMap)
                }
            }
            .setNegativeButton(R.string.dialog_cancel)
            .create()
            .show(fragmentManager, null)
    }
}

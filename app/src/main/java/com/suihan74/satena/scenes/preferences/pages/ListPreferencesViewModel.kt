package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TextIdContainer
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences

abstract class ListPreferencesViewModel(
    context: Context
) : PreferencesViewModel<PreferenceKey>(SafeSharedPreferences.create(context)) {

    /**
     * 設定リストを生成する
     */
    abstract fun createList(
        activity: PreferencesActivity,
        fragment: Fragment
    ) : List<PreferencesAdapter.Item>

    /**
     * Enum値を選択するダイアログを開く
     */
    fun <T> openEnumSelectionDialog(
        values: Array<T>,
        liveData: MutableLiveData<T>,
        @StringRes titleId: Int,
        fragmentManager: FragmentManager,
        onSelected: ((f: AlertDialogFragment, old: T, new: T)->Unit)? = null
    ) where T: Enum<T>, T: TextIdContainer {
        val labelIds = values.map { it.textId }
        val old = liveData.value as T
        val initialSelected = values.indexOf(old)

        AlertDialogFragment.Builder()
            .setTitle(titleId)
            .setSingleChoiceItems(labelIds, initialSelected) { f, which ->
                val new = values[which]
                liveData.value = new
                onSelected?.invoke(f, old, new)
            }
            .dismissOnClickItem(true)
            .setNegativeButton(R.string.dialog_cancel)
            .create()
            .show(fragmentManager, null)
    }
}

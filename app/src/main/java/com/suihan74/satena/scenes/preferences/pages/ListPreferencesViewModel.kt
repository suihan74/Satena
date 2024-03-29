package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.TextIdContainer
import com.suihan74.satena.scenes.preferences.PreferencesAdapter
import com.suihan74.satena.scenes.preferences.PreferencesViewModel
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * 設定リスト画面用ベースViewModel
 */
abstract class ListPreferencesViewModel(
    context: Context
) : PreferencesViewModel<PreferenceKey>(SafeSharedPreferences.create(context)) {

    private val _preferencesItems = MutableLiveData<List<PreferencesAdapter.Item>>()
    val preferencesItems : LiveData<List<PreferencesAdapter.Item>> = _preferencesItems

    /** 設定リストを作成しビューに反映する */
    fun load(fragment: ListPreferencesFragment) {
        fragment.lifecycleScope.launchWhenCreated {
            withContext(Dispatchers.IO) {
                _preferencesItems.postValue(
                    createList(fragment.requireActivity(), fragment.childFragmentManager)
                )
            }
        }
    }

    // ------ //

    @MainThread
    open fun onCreateView(fragment: ListPreferencesFragment) {
        load(fragment)
    }

    /**
     * 設定リストを生成する
     *
     * `load()`メソッドから呼ばれる場合，
     * `fragment.lifecycleScope.launchWhenCreated {}` 内で呼ばれることを保証している
     *
     */
    abstract fun createList(
        context: Context,
        fragmentManager: FragmentManager
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

    /**
     * Enum値を選択するダイアログを開く
     */
    fun <T> openEnumSelectionDialog(
        values: Array<T>,
        liveData: MutableStateFlow<T>,
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

    /** 数値を選択するダイアログを開く */
    fun openNumberPickerDialog(
        liveData: MutableLiveData<Int>,
        @StringRes titleId: Int,
        @StringRes messageId: Int?,
        fragmentManager: FragmentManager,
        min: Int,
        max: Int,
        onComplete: ((value: Int)->Unit)? = null
    ) {
        val dialog = NumberPickerDialog.createInstance(
            min = min,
            max = max,
            default = liveData.value!!,
            titleId = titleId,
            messageId = messageId
        ) { value ->
            liveData.value = value
            onComplete?.invoke(value)
        }
        dialog.show(fragmentManager, null)
    }

    /** 数値を選択するダイアログを開く */
    fun openNumberPickerDialog(
        liveData: MutableLiveData<Long>,
        @StringRes titleId: Int,
        @StringRes messageId: Int?,
        fragmentManager: FragmentManager,
        min: Long,
        max: Long,
        onComplete: ((value: Long)->Unit)? = null
    ) {
        val dialog = NumberPickerDialog.createInstance(
            min = min.toInt(),
            max = max.toInt(),
            default = liveData.value!!.toInt(),
            titleId = titleId,
            messageId = messageId
        ) { value ->
            val valueLong = value.toLong()
            liveData.value = valueLong
            onComplete?.invoke(valueLong)
        }
        dialog.show(fragmentManager, null)
    }
}

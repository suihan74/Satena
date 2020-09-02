package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.InverseMethod
import androidx.lifecycle.ViewModelProvider
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesEntriesBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.entries2.AdditionalBottomItemsAlignment
import com.suihan74.satena.scenes.entries2.CategoriesMode
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import kotlinx.android.synthetic.main.fragment_preferences_entries.view.*

class PreferencesEntriesFragment :
    PreferencesFragmentBase(),
    AlertDialogFragment.Listener,
    NumberPickerDialogFragment.Listener
{
    companion object {
        fun createInstance() = PreferencesEntriesFragment()

        private const val DIALOG_SINGLE_TAP_ACTION = "DIALOG_SINGLE_TAP_ACTION"
        private const val DIALOG_LONG_TAP_ACTION = "DIALOG_LONG_TAP_ACTION"
        private const val DIALOG_HOME_CATEGORY = "DIALOG_HOME_CATEGORY"
        private const val DIALOG_HOME_TAB = "DIALOG_HOME_TAB"
        private const val DIALOG_HISTORY_MAX_SIZE_PICKER = "DIALOG_HISTORY_MAX_SIZE_PICKER"
        private const val DIALOG_ENTRY_READ_ACTION_TYPE = "DIALOG_ENTRY_READ_ACTION_TYPE"
        private const val DIALOG_CATEGORIES_MODE = "DIALOG_CATEGORIES_MODE"
        private const val DIALOG_ADDITIONAL_BOTTOM_ITEMS_ALIGNMENT = "DIALOG_ADDITIONAL_BOTTOM_ITEMS_ALIGNMENT"
    }

    val viewModel : PreferencesEntriesViewModel by lazy {
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
        val factory = PreferencesEntriesViewModel.Factory(prefs, historyPrefs)
        ViewModelProvider(this, factory)[PreferencesEntriesViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesEntriesBinding>(
            inflater,
            R.layout.fragment_preferences_entries,
            container,
            false
        ).apply {
            vm = viewModel
            fragmentManager = childFragmentManager
            lifecycleOwner = viewLifecycleOwner
        }
        val view = binding.root

        val tapActions = TapEntryAction.values().map { getString(it.titleId) }.toTypedArray()

        // シングルタップ時の動作
        view.preferences_entries_single_tap_action.setOnClickListener {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_entries_single_tap_action_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(tapActions, viewModel.singleTapAction.value!!.ordinal)
                .showAllowingStateLoss(childFragmentManager, DIALOG_SINGLE_TAP_ACTION)
        }

        // ロングタップ時の動作
        view.preferences_entries_long_tap_action.setOnClickListener {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_entries_long_tap_action_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(tapActions, viewModel.longTapAction.value!!.ordinal)
                .showAllowingStateLoss(childFragmentManager, DIALOG_LONG_TAP_ACTION)
        }

        // ホームカテゴリ
        view.preferences_home_category.setOnClickListener {
            val categories = (
                if (HatenaClient.signedIn()) Category.valuesWithSignedIn()
                else Category.valuesWithoutSignedIn()
            ).filter { it.willBeHome }

            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_home_category_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setAdditionalData("categories", categories)
                .setSingleChoiceItems(
                    categories.map { getString(it.textId) },
                    categories.indexOfFirst { it.id == viewModel.homeCategory.value!!.id }
                )
                .showAllowingStateLoss(childFragmentManager, DIALOG_HOME_CATEGORY)
        }

        // 最初に表示するタブ
        view.preferences_entries_initial_tab.setOnClickListener {
            val items = getTabTitleIds(viewModel.homeCategory.value!!)

            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_entries_initial_tab_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    items.map { getString(it) },
                    viewModel.initialTab.value!!)
                .showAllowingStateLoss(childFragmentManager, DIALOG_HOME_TAB)
        }

        // ブクマ閲覧履歴の最大保存数
        view.preferences_entries_history_max_size.setOnClickListener {
            NumberPickerDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_entries_history_max_size_dialog_title)
                .setMessage(R.string.pref_entries_history_max_size_dialog_msg)
                .setMinValue(1)
                .setMaxValue(100)
                .setDefaultValue(viewModel.historyMaxSize.value!!)
                .showAllowingStateLoss(childFragmentManager, DIALOG_HISTORY_MAX_SIZE_PICKER)
        }

        // 「あとで読む」エントリを「読んだ」したときの挙動
        view.preferences_entries_read_action_type.setOnClickListener {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_entries_read_action_type_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    EntryReadActionType.values().map { getString(it.textId) }.toTypedArray(),
                    viewModel.entryReadActionType.value!!.ordinal)
                .showAllowingStateLoss(childFragmentManager, DIALOG_ENTRY_READ_ACTION_TYPE)
        }

        // カテゴリリストの表示形式
        view.preferences_entries_categories_mode.setOnClickListener {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_entries_categories_mode_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    CategoriesMode.values().map { getString(it.textId) }.toTypedArray(),
                    viewModel.categoriesMode.value!!.ordinal)
                .show(childFragmentManager, DIALOG_CATEGORIES_MODE)
        }

        // ボトムバーの追加項目の配置方法
        view.additional_bottom_items_alignment_button.setOnClickListener {
            AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                .setTitle(R.string.pref_additional_bottom_items_alignment_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    AdditionalBottomItemsAlignment.values().map { getString(it.textId) }.toTypedArray(),
                    viewModel.additionalBottomItemsAlignment.value!!.ordinal
                )
                .show(childFragmentManager, DIALOG_ADDITIONAL_BOTTOM_ITEMS_ALIGNMENT)
        }

        return view
    }

    /** ダイアログ項目の選択 */
    override fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {
        when (dialog.tag) {
            DIALOG_SINGLE_TAP_ACTION ->
                viewModel.singleTapAction.value = TapEntryAction.fromInt(which)

            DIALOG_LONG_TAP_ACTION ->
                viewModel.longTapAction.value = TapEntryAction.fromOrdinal(which)

            DIALOG_HOME_CATEGORY -> {
                val categories = dialog.getAdditionalData<Array<Category>>("categories")!!
                val cat = categories[which]
                viewModel.homeCategory.value = cat
            }

            DIALOG_HOME_TAB ->
                viewModel.initialTab.value = which

            DIALOG_ENTRY_READ_ACTION_TYPE ->
                viewModel.entryReadActionType.value = EntryReadActionType.fromInt(which)

            DIALOG_CATEGORIES_MODE ->
                viewModel.categoriesMode.value = CategoriesMode.fromInt(which)

            DIALOG_ADDITIONAL_BOTTOM_ITEMS_ALIGNMENT ->
                viewModel.additionalBottomItemsAlignment.value = AdditionalBottomItemsAlignment.values()[which]
        }
        dialog.dismiss()
    }

    /** NumberPickerの処理完了 */
    override fun onCompleteNumberPicker(value: Int, dialog: NumberPickerDialogFragment) {
        viewModel.historyMaxSize.value = value
    }
}

/** タブ名をカテゴリによって変化させる */
private fun getTabTitleIds(homeCategory: Category) : List<Int> =
    when (homeCategory) {
        Category.MyBookmarks -> listOf(R.string.entries_tab_mybookmarks, R.string.entries_tab_read_later)
        Category.Stars -> listOf(R.string.entries_tab_my_stars, R.string.entries_tab_stars_report)
        else -> listOf(R.string.entries_tab_hot, R.string.entries_tab_recent)
    }

/** 初期表示タブ名 */
@BindingAdapter("initialTab", "homeCategory")
fun Button.setInitialTabText(position: Int, homeCategory: Category) {
    val initialTabs = getTabTitleIds(homeCategory)
    setText(initialTabs[position])
}


object BottomMenuItemsGravityConverter {
    @InverseMethod("inverseSet")
    @JvmStatic
    fun set(new: Int?) : Boolean =
        new == Gravity.END

    @JvmStatic
    fun inverseSet(new: Boolean?) : Int =
        if (new == true) Gravity.END else Gravity.START
}

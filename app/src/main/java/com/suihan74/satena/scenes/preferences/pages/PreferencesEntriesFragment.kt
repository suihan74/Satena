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
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentPreferencesEntriesBinding
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.NumberPickerDialog
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.entries2.CategoriesMode
import com.suihan74.satena.scenes.entries2.ExtraBottomItemsAlignment
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.provideViewModel
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.android.synthetic.main.fragment_preferences_entries.view.*

class PreferencesEntriesFragment : PreferencesFragmentBase() {
    companion object {
        fun createInstance() = PreferencesEntriesFragment()
    }

    val viewModel by lazy {
        provideViewModel(this) {
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val historyPrefs = SafeSharedPreferences.create<EntriesHistoryKey>(context)
            PreferencesEntriesViewModel(prefs, historyPrefs)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesEntriesBinding>(
            inflater,
            R.layout.fragment_preferences_entries,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        val view = binding.root

        val tapActions = TapEntryAction.values().map { it.titleId }

        // ボトムバーメニュー項目を編集するダイアログを表示する
        view.bottom_bar_item_setter.setOnMenuItemClickListener { args ->
            viewModel.showBottomBarItemSetterDialog(
                args,
                childFragmentManager
            )
        }

        // シングルタップ時の動作
        view.preferences_entries_single_tap_action.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_entries_single_tap_action_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(tapActions, viewModel.singleTapAction.value!!.ordinal) { _, which ->
                    viewModel.singleTapAction.value = TapEntryAction.fromOrdinal(which)
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        // 複数回タップ時の動作
        view.preferences_entries_multiple_tap_action.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_entries_multiple_tap_action_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(tapActions, viewModel.multipleTapAction.value!!.ordinal) { _, which ->
                    viewModel.multipleTapAction.value = TapEntryAction.fromOrdinal(which)
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        // ロングタップ時の動作
        view.preferences_entries_long_tap_action.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_entries_long_tap_action_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(tapActions, viewModel.longTapAction.value!!.ordinal) { _, which ->
                    viewModel.longTapAction.value = TapEntryAction.fromOrdinal(which)
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        view.preferences_entries_multiple_tap_duration.setOnClickListener {
            val dialog = NumberPickerDialog.createInstance(
                min = 0,
                max = 500,
                default = viewModel.multipleTapDuration.value!!.toInt(),
                titleId = R.string.pref_entries_multiple_tap_duration_desc,
                messageId = R.string.pref_entries_multiple_tap_duration_dialog_message
            ) { value ->
                viewModel.multipleTapDuration.value = value.toLong()
            }
            dialog.showAllowingStateLoss(childFragmentManager)
        }

        // ホームカテゴリ
        view.preferences_home_category.setOnClickListener {
            val categories = (
                if (HatenaClient.signedIn()) Category.valuesWithSignedIn()
                else Category.valuesWithoutSignedIn()
            ).filter { it.willBeHome }

            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_home_category_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    categories.map { it.textId },
                    categories.indexOfFirst { it.id == viewModel.homeCategory.value!!.id }
                ) { _, which ->
                    viewModel.homeCategory.value = categories[which]
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        // 最初に表示するタブ
        view.preferences_entries_initial_tab.setOnClickListener {
            val items = getTabTitleIds(viewModel.homeCategory.value!!)

            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_entries_initial_tab_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    items,
                    viewModel.initialTab.value!!
                ) { _, which ->
                    viewModel.initialTab.value = which
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        // ブクマ閲覧履歴の最大保存数
        view.preferences_entries_history_max_size.setOnClickListener {
            val dialog = NumberPickerDialog.createInstance(
                min = 1,
                max = 100,
                default = viewModel.historyMaxSize.value!!,
                titleId = R.string.pref_entries_history_max_size_dialog_title,
                messageId = R.string.pref_entries_history_max_size_dialog_msg
            ) { value ->
                viewModel.multipleTapDuration.value = value.toLong()
            }
            dialog.showAllowingStateLoss(childFragmentManager)
        }

        // 「あとで読む」エントリを「読んだ」したときの挙動
        view.preferences_entries_read_action_type.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_entries_read_action_type_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    EntryReadActionType.values().map { it.textId },
                    viewModel.entryReadActionType.value!!.ordinal
                ) { _, which ->
                    viewModel.entryReadActionType.value = EntryReadActionType.fromOrdinal(which)
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        // カテゴリリストの表示形式
        view.preferences_entries_categories_mode.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_entries_categories_mode_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    CategoriesMode.values().map { it.textId },
                    viewModel.categoriesMode.value!!.ordinal
                ) { _, which ->
                    viewModel.categoriesMode.value = CategoriesMode.fromOrdinal(which)
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        // ボトムバーの追加項目の配置方法
        view.extra_bottom_items_alignment_button.setOnClickListener {
            AlertDialogFragment.Builder()
                .setTitle(R.string.pref_extra_bottom_items_alignment_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setSingleChoiceItems(
                    ExtraBottomItemsAlignment.values().map { it.textId },
                    viewModel.extraBottomItemsAlignment.value!!.ordinal
                ) { _, which ->
                    viewModel.extraBottomItemsAlignment.value = ExtraBottomItemsAlignment.fromOrdinal(which)
                }
                .dismissOnClickItem(true)
                .create()
                .showAllowingStateLoss(childFragmentManager)
        }

        return view
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

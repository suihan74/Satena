package com.suihan74.satena.scenes.preferences.bottomBar

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.UserBottomItem
import com.suihan74.satena.scenes.preferences.pages.PreferencesEntriesFragment
import com.suihan74.utilities.*

class BottomBarItemSelectionDialog : DialogFragment() {
    companion object {
        fun createInstance(
            existedItems: List<UserBottomItem>,
            targetItem: UserBottomItem? = null
        ) = BottomBarItemSelectionDialog().withArguments {
            putObject(ARG_EXISTED_ITEMS, existedItems)
            putEnum(ARG_TARGET_ITEMS, targetItem)
        }

        private const val ARG_EXISTED_ITEMS = "ARG_EXISTED_ITEM"
        private const val ARG_TARGET_ITEMS = "ARG_TARGET_ITEM"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val existedItems = args.getObject<List<UserBottomItem>>(ARG_EXISTED_ITEMS)!!
        val targetItem = args.getEnum<UserBottomItem>(ARG_TARGET_ITEMS)

        // 編集対象のメニュー項目位置
        val targetPosition =
            if (targetItem == null) existedItems.size
            else existedItems.indexOf(targetItem)

        // 選択可能なアイテムリスト
        val items = UserBottomItem.values().filterNot {
            existedItems.contains(it) && it != targetItem
        }
        val itemLabels = items.map { getString(it.textId) }.toTypedArray()

        // 現在設定されているアイテム位置
        val checkedPosition = items.indexOf(targetItem)

        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.dialog_title_bottom_bar_item_selection)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setSingleChoiceItems(itemLabels, checkedPosition) { dialog, which ->
                val listener = (parentFragment as? PreferencesEntriesFragment)?.viewModel as? Listener
                val new = items[which]
                listener?.onSelectItem(targetPosition, targetItem, new)
                dismissAllowingStateLoss()
            }

        if (targetItem != null) {
            dialogBuilder.setPositiveButton(R.string.dialog_delete) { dialog, which ->
                val listener = (parentFragment as? PreferencesEntriesFragment)?.viewModel as? Listener
                listener?.onSelectItem(targetPosition, targetItem, null)
            }
        }

        return dialogBuilder.create()
    }

    interface Listener {
        fun onSelectItem(position: Int, old: UserBottomItem?, new: UserBottomItem?)
    }
}

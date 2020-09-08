package com.suihan74.satena.scenes.preferences.bottomBar

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.style.ImageSpan
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.whenStarted
import com.suihan74.satena.R
import com.suihan74.satena.scenes.entries2.UserBottomItem
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

    private val viewModel: DialogViewModel by lazy {
        ViewModelProvider(this)[DialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val args = requireArguments()
        val existedItems = args.getObject<List<UserBottomItem>>(ARG_EXISTED_ITEMS)!!
        val targetItem = args.getEnum<UserBottomItem>(ARG_TARGET_ITEMS)

        // 編集対象のメニュー項目位置
        val targetPosition =
            if (targetItem == null) existedItems.size
            else existedItems.indexOf(targetItem)

        // 選択可能なアイテムリスト
        val items = UserBottomItem.values()
        val itemLabels = items.map { createLabel(context, it) }.toTypedArray()

        // 現在設定されているアイテム位置
        val checkedPosition = items.indexOf(targetItem)

        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.dialog_title_bottom_bar_item_selection)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setSingleChoiceItems(itemLabels, checkedPosition) { _, which ->
                val new = items[which]

                val existedPosition = existedItems.indexOf(new)
                if (existedPosition == -1) {
                    viewModel.onSelectItem?.invoke(
                        OnSelectItemArguments(targetPosition, targetItem, new)
                    )
                }
                else if (existedPosition != targetPosition) {
                    viewModel.onReorderItem?.invoke(
                        OnReorderItemArguments(targetPosition, existedPosition, targetItem, new)
                    )
                }

                dismissAllowingStateLoss()
            }

        if (targetItem != null) {
            // 項目を削除する
            dialogBuilder.setPositiveButton(R.string.dialog_delete) { _, _ ->
                viewModel.onSelectItem?.invoke(
                    OnSelectItemArguments(targetPosition, targetItem, null)
                )
            }
        }

        return dialogBuilder.create()
    }

    private fun createLabel(context: Context, item: UserBottomItem) =
        buildSpannedString {
            ContextCompat.getDrawable(context, item.iconId)?.let { icon ->
                val lineHeight = context.sp2px(18)
                val vAlign =
                    if (Build.VERSION.SDK_INT >= 29) ImageSpan.ALIGN_CENTER
                    else ImageSpan.ALIGN_BASELINE

                icon.setTint(ContextCompat.getColor(context, R.color.textColor))
                icon.setBounds(0, 0, lineHeight, lineHeight)
                append("_", ImageSpan(icon, vAlign))
                append("\u2002") // for margin
            }
            append(getString(item.textId))
        }

    // ------ //

    data class OnSelectItemArguments(
        val position: Int,
        val old: UserBottomItem?,
        val new: UserBottomItem?
    )

    data class OnReorderItemArguments(
        val posA: Int,
        val posB: Int,
        val itemA: UserBottomItem?,
        val itemB: UserBottomItem?
    )

    suspend fun setOnSelectItemListener(
        listener: Listener<OnSelectItemArguments>? = null
    ) = whenStarted {
        viewModel.onSelectItem = listener
    }

    suspend fun setOnReorderItemListener(
        listener: Listener<OnReorderItemArguments>? = null
    ) = whenStarted {
        viewModel.onReorderItem = listener
    }

    class DialogViewModel : ViewModel() {
        /** アイテムを設定(または変更) */
        var onSelectItem: Listener<OnSelectItemArguments>? = null

        /** ふたつのアイテムを入れ替える */
        var onReorderItem: Listener<OnReorderItemArguments>? = null
    }
}

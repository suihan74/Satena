package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.Spannable
import android.text.style.AbsoluteSizeSpan
import androidx.appcompat.app.AlertDialog
import androidx.core.text.buildSpannedString
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.whenStarted
import com.suihan74.hatenaLib.Star
import com.suihan74.satena.R
import com.suihan74.utilities.*

class StarDeletionDialog : DialogFragment() {
    companion object {
        /** @param stars 削除対象のスターリスト */
        fun createInstance(stars: List<Star>) = StarDeletionDialog().withArguments {
            putObject(ARG_STARS, stars)
        }

        private const val ARG_STARS = "ARG_STARS"
    }

    private val viewModel: DialogViewModel by lazy {
        val args = requireArguments()
        val factory = DialogViewModel.Factory(args.getObject(ARG_STARS)!!)
        ViewModelProvider(this, factory)[DialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val items = viewModel.stars.map { star ->
            buildSpannedString {
                val length = appendStarSpan(this, star.count, context, star.styleId)
                setSpan(
                    AbsoluteSizeSpan(20, true),
                    this.length - length,
                    this.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.dialog_title_star_deletion)
            .setMultiChoiceItems(items.toTypedArray(), viewModel.checkedArray) { _, idx, checked ->
                viewModel.checkedArray[idx] = checked
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_delete, null)
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (!viewModel.checkedArray.contains(true)) {
                        requireContext().showToast(R.string.msg_no_deleting_star_selected)
                    }
                    else {
                        viewModel.onDeleteStars?.invoke(viewModel.selectedStars)
                        dismiss()
                    }
                }
            }
    }

    /** 削除するスターが決定したときの処理をセットする */
    suspend fun setOnDeleteStars(listener: Listener<List<Star>>?) = whenStarted {
        viewModel.onDeleteStars = listener
    }

    // ------ //

    class DialogViewModel(
        /** 候補のスターリスト */
        val stars: List<Star>
    ) : androidx.lifecycle.ViewModel() {

        /** 削除するスターが決定したときの処理 */
        var onDeleteStars: Listener<List<Star>>? = null

        val checkedArray = BooleanArray(stars.size) { idx -> idx == 0 }

        /** 選択結果 */
        val selectedStars: List<Star>
            get() = stars.filterIndexed { idx, _ -> checkedArray[idx] }

        class Factory(
            private val stars:List<Star>
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return DialogViewModel(stars) as T
            }
        }
    }
}
@file:Suppress("unused")

package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.onNot
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class AlertDialogFragment2 : DialogFragment() {
    companion object {
        private fun createInstance() = AlertDialogFragment2().withArguments()

        private const val ARG_TITLE_ID = "ARG_TITLE_ID"
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_MESSAGE_ID = "ARG_MESSAGE_ID"
        private const val ARG_MESSAGE = "ARG_MESSAGE"
        private const val ARG_POSITIVE_BUTTON_TEXT_ID = "ARG_POSITIVE_BUTTON_TEXT_ID"
        private const val ARG_NEGATIVE_BUTTON_TEXT_ID = "ARG_NEGATIVE_BUTTON_TEXT_ID"
        private const val ARG_NEUTRAL_BUTTON_TEXT_ID = "ARG_NEUTRAL_BUTTON_TEXT_ID"
        private const val ARG_ITEM_LABEL_IDS = "ARG_ITEM_LABEL_IDS"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            DialogViewModel().also { vm ->
                val args = requireArguments()

                vm.titleId = args.getInt(ARG_TITLE_ID, 0)
                vm.title = args.getCharSequence(ARG_TITLE)
                vm.messageId = args.getInt(ARG_MESSAGE_ID, 0)
                vm.message = args.getCharSequence(ARG_MESSAGE)
                vm.positiveTextId = args.getInt(ARG_POSITIVE_BUTTON_TEXT_ID, 0)
                vm.negativeTextId = args.getInt(ARG_NEGATIVE_BUTTON_TEXT_ID, 0)
                vm.neutralTextId = args.getInt(ARG_NEUTRAL_BUTTON_TEXT_ID, 0)
                vm.itemLabelIds = args.getIntArray(ARG_ITEM_LABEL_IDS)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val builder = AlertDialog.Builder(context, R.style.AlertDialogStyle)

        viewModel.titleId.onNot(0) {
            builder.setTitle(it)
        }

        viewModel.title?.let {
            builder.setTitle(it)
        }

        viewModel.messageId.onNot(0) {
            builder.setMessage(it)
        }

        viewModel.message?.let {
            builder.setMessage(it)
        }

        viewModel.positiveTextId.onNot(0) {
            builder.setPositiveButton(it, null)
        }

        viewModel.negativeTextId.onNot(0) {
            builder.setNegativeButton(it, null)
        }

        viewModel.neutralTextId.onNot(0) {
            builder.setNeutralButton(it, null)
        }

        viewModel.itemLabelIds?.let { ids ->
            val labels = ids.map { getText(it) }.toTypedArray()
            builder.setItems(labels, null)
        }

        val dialog = builder.show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
            viewModel.onClickPositiveButton?.invoke(this)
            if (viewModel.dismissOnClickButton) {
                dismiss()
            }
        }

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setOnClickListener {
            viewModel.onClickNegativeButton?.invoke(this)
            if (viewModel.dismissOnClickButton) {
                dismiss()
            }
        }

        dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
            viewModel.onClickNeutralButton?.invoke(this)
            if (viewModel.dismissOnClickButton) {
                dismiss()
            }
        }

        dialog.listView?.setOnItemClickListener { adapterView, view, i, l ->
            viewModel.onClickItem?.invoke(this, i)
            if (false != viewModel.dismissOnClickItem) {
                dismiss()
            }
        }

        // TODO:
        dialog.listView?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, viwe: View?, i: Int, l: Long) {
                viewModel.onClickItem?.invoke(this@AlertDialogFragment2, i)
                if (true == viewModel.dismissOnClickItem) {
                    dismiss()
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        return dialog
    }

    // ------ //

    fun setDismissOnClickButton(flag: Boolean) = lifecycleScope.launchWhenStarted {
        viewModel.dismissOnClickButton = flag
    }

    fun setDismissOnClickItem(flag: Boolean) = lifecycleScope.launchWhenStarted {
        viewModel.dismissOnClickItem = flag
    }

    fun setOnClickPositiveButton(listener: Listener<AlertDialogFragment2>?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickPositiveButton = listener
    }

    fun setOnClickNegativeButton(listener: Listener<AlertDialogFragment2>?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickNegativeButton = listener
    }

    fun setOnClickNeutralButton(listener: Listener<AlertDialogFragment2>?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickNeutralButton = listener
    }

    fun setOnClickItem(listener: ((AlertDialogFragment2, Int)->Unit)?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickItem = listener
    }

    // ------ //

    class DialogViewModel : ViewModel() {
        @StringRes
        var titleId : Int = 0

        var title : CharSequence? = null

        @StringRes
        var messageId : Int = 0

        var message : CharSequence? = null

        @StringRes
        var positiveTextId : Int = 0

        @StringRes
        var negativeTextId : Int = 0

        @StringRes
        var neutralTextId : Int = 0

        var itemLabelIds : IntArray? = null

        /** ボタンクリック処理後に自動でダイアログを閉じる */
        var dismissOnClickButton : Boolean = true

        /** 項目クリック処理後に自動でダイアログを閉じる (null: choiceItems時にはfalse, Items時にはtrue) */
        var dismissOnClickItem : Boolean? = null

        /** ポジティブボタンのクリック時処理 */
        var onClickPositiveButton : Listener<AlertDialogFragment2>? = null

        /** ネガティブボタンのクリック時処理 */
        var onClickNegativeButton : Listener<AlertDialogFragment2>? = null

        /** ニュートラルボタンのクリック時処理 */
        var onClickNeutralButton : Listener<AlertDialogFragment2>? = null

        /** リスト項目のクリック時処理 */
        var onClickItem : ((AlertDialogFragment2, Int)->Unit)? = null
    }

    // ------ //

    class Builder(
        private val styleId: Int = R.style.AlertDialogStyle
    ) {
        private val dialog = AlertDialogFragment2.createInstance()
        private val args = dialog.requireArguments()

        fun create() = dialog

        fun setTitle(@StringRes titleId: Int) : Builder {
            args.putInt(ARG_TITLE_ID, titleId)
            return this
        }

        fun setMessage(@StringRes messageId: Int) : Builder {
            args.putInt(ARG_MESSAGE_ID, messageId)
            return this
        }

        fun setTitle(title: CharSequence) : Builder {
            args.putCharSequence(ARG_TITLE, title)
            return this
        }

        fun setMessage(message: CharSequence) : Builder {
            args.putCharSequence(ARG_MESSAGE, message)
            return this
        }

        fun setPositiveButton(@StringRes textId: Int, listener: Listener<AlertDialogFragment2>? = null) : Builder {
            args.putInt(ARG_POSITIVE_BUTTON_TEXT_ID, textId)
            dialog.setOnClickPositiveButton(listener)
            return this
        }

        fun setNegativeButton(@StringRes textId: Int, listener: Listener<AlertDialogFragment2>? = null) : Builder {
            args.putInt(ARG_NEGATIVE_BUTTON_TEXT_ID, textId)
            dialog.setOnClickNegativeButton(listener)
            return this
        }

        fun setNeutralButton(@StringRes textId: Int, listener: Listener<AlertDialogFragment2>? = null) : Builder {
            args.putInt(ARG_NEUTRAL_BUTTON_TEXT_ID, textId)
            dialog.setOnClickNeutralButton(listener)
            return this
        }

        fun dismissOnClickButton(flag: Boolean) : Builder {
            dialog.setDismissOnClickButton(flag)
            return this
        }

        fun dismissOnClickItem(flag: Boolean) : Builder {
            dialog.setDismissOnClickItem(flag)
            return this
        }

        fun setItems(
            labelIds: List<Int>,
            listener: ((dialog: AlertDialogFragment2, which: Int)->Unit)? = null
        ) : Builder {
            args.putIntArray(ARG_ITEM_LABEL_IDS, labelIds.toIntArray())
            dialog.setOnClickItem(listener)
            return this
        }
    }
}


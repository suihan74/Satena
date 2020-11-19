@file:Suppress("unused")

package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.provideViewModel

/**
 * 汎用的なダイアログフラグメント
 *
 * `AlertDialog`を`DialogFragment`で包んで作成し，扱うデータを`lifecycle`内で持続させる
 */
class AlertDialogFragment : DialogFragment() {
    companion object {
        private fun createInstance() = AlertDialogFragment().withArguments()

        private const val ARG_THEME_ID = "ARG_THEME_ID"
        private const val ARG_TITLE_ID = "ARG_TITLE_ID"
        private const val ARG_TITLE = "ARG_TITLE"
        private const val ARG_MESSAGE_ID = "ARG_MESSAGE_ID"
        private const val ARG_MESSAGE = "ARG_MESSAGE"
        private const val ARG_POSITIVE_BUTTON_TEXT_ID = "ARG_POSITIVE_BUTTON_TEXT_ID"
        private const val ARG_NEGATIVE_BUTTON_TEXT_ID = "ARG_NEGATIVE_BUTTON_TEXT_ID"
        private const val ARG_NEUTRAL_BUTTON_TEXT_ID = "ARG_NEUTRAL_BUTTON_TEXT_ID"
        private const val ARG_ITEM_LABEL_IDS = "ARG_ITEM_LABEL_IDS"
        private const val ARG_ITEM_LABELS = "ARG_ITEM_LABELS"
        private const val ARG_ITEMS_MODE = "ARG_ITEMS_MODE"
        private const val ARG_SINGLE_CHECKED_ITEM = "ARG_SINGLE_CHECKED_ITEM"
        private const val ARG_MULTI_CHECKED_ITEMS = "ARG_MULTI_CHECKED_ITEMS"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            DialogViewModel(requireArguments())
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return viewModel.createDialog(this)
    }

    // ------ //

    /** singleChoiceItemsで選択されている項目 */
    val checkedItem : Int
        get() = viewModel.checkedItem

    /** multiChoiceItemsでの各項目の選択状態 */
    val checkedItems : BooleanArray
        get() = viewModel.checkedItems

    // ------ //

    fun setDismissOnClickButton(flag: Boolean) = lifecycleScope.launchWhenStarted {
        viewModel.dismissOnClickButton = flag
    }

    fun setDismissOnClickItem(flag: Boolean) = lifecycleScope.launchWhenStarted {
        viewModel.dismissOnClickItem = flag
    }

    fun setOnClickPositiveButton(listener: Listener<AlertDialogFragment>?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickPositiveButton = listener
    }

    fun setOnClickNegativeButton(listener: Listener<AlertDialogFragment>?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickNegativeButton = listener
    }

    fun setOnClickNeutralButton(listener: Listener<AlertDialogFragment>?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickNeutralButton = listener
    }

    fun setOnClickItem(listener: ((AlertDialogFragment, Int)->Unit)?) = lifecycleScope.launchWhenStarted {
        viewModel.onClickItem = listener
    }

    // ------ //

    class DialogViewModel(args: Bundle) : ViewModel() {
        @StyleRes
        val themeId: Int? = args.getIntOrNull(ARG_THEME_ID)

        @StringRes
        val titleId : Int = args.getInt(ARG_TITLE_ID, 0)

        val title : CharSequence? = args.getCharSequence(ARG_TITLE)

        @StringRes
        val messageId : Int = args.getInt(ARG_MESSAGE_ID, 0)

        val message : CharSequence? = args.getCharSequence(ARG_MESSAGE)

        @StringRes
        val positiveTextId : Int = args.getInt(ARG_POSITIVE_BUTTON_TEXT_ID, 0)

        @StringRes
        val negativeTextId : Int = args.getInt(ARG_NEGATIVE_BUTTON_TEXT_ID, 0)

        @StringRes
        val neutralTextId : Int = args.getInt(ARG_NEUTRAL_BUTTON_TEXT_ID, 0)

        /**
         *  各項目ラベル文字列リソースID
         *
         * itemLabelsより優先される
         */
        val itemLabelIds : IntArray? = args.getIntArray(ARG_ITEM_LABEL_IDS)

        /** 各項目ラベル文字列 */
        val itemLabels : Array<out CharSequence>? = args.getCharSequenceArray(ARG_ITEM_LABELS)

        /**
         * 項目の表示モード
         */
        val itemsMode : ItemsMode = args.getEnum(ARG_ITEMS_MODE, ItemsMode.SINGLE_CLICK)

        /** singleChoiceItemsの選択項目位置 */
        var checkedItem : Int = args.getInt(ARG_SINGLE_CHECKED_ITEM, 0)
            private set

        /** multiChoiceItemsの選択項目位置 */
        val checkedItems : BooleanArray by lazy {
            args.getBooleanArray(ARG_MULTI_CHECKED_ITEMS) ?: BooleanArray(0)
        }

        /** ボタンクリック処理後に自動でダイアログを閉じる */
        var dismissOnClickButton : Boolean = true

        /** 項目クリック処理後に自動でダイアログを閉じる (null: choiceItems時にはfalse, Items時にはtrue) */
        var dismissOnClickItem : Boolean? = null

        /** ポジティブボタンのクリック時処理 */
        var onClickPositiveButton : Listener<AlertDialogFragment>? = null

        /** ネガティブボタンのクリック時処理 */
        var onClickNegativeButton : Listener<AlertDialogFragment>? = null

        /** ニュートラルボタンのクリック時処理 */
        var onClickNeutralButton : Listener<AlertDialogFragment>? = null

        /** リスト項目のクリック時処理 */
        var onClickItem : ((AlertDialogFragment, Int)->Unit)? = null

        // ------ //

        fun createDialog(fragment: AlertDialogFragment) : AlertDialog {
            val builder = fragment.createBuilder(fragment.requireContext(), themeId)

            titleId.onNot(0) {
                builder.setTitle(it)
            }

            title?.let {
                builder.setTitle(it)
            }

            messageId.onNot(0) {
                builder.setMessage(it)
            }

            message?.let {
                builder.setMessage(it)
            }

            positiveTextId.onNot(0) {
                builder.setPositiveButton(it, null)
            }

            negativeTextId.onNot(0) {
                builder.setNegativeButton(it, null)
            }

            neutralTextId.onNot(0) {
                builder.setNeutralButton(it, null)
            }

            // 項目の初期化
            initializeItems(fragment, builder)

            val dialog = builder.show()

            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                onClickPositiveButton?.invoke(fragment)
                if (dismissOnClickButton) {
                    fragment.dismiss()
                }
            }

            dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setOnClickListener {
                onClickNegativeButton?.invoke(fragment)
                if (dismissOnClickButton) {
                    fragment.dismiss()
                }
            }

            dialog.getButton(DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                onClickNeutralButton?.invoke(fragment)
                if (dismissOnClickButton) {
                    fragment.dismiss()
                }
            }

            dialog.listView?.setOnItemClickListener { adapterView, view, i, l ->
                onClickItem?.invoke(fragment, i)
                if (false != dismissOnClickItem) {
                    fragment.dismiss()
                }
            }

            dialog.listView?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    viwe: View?,
                    i: Int,
                    l: Long
                ) {
                    checkedItem = i
                    onClickItem?.invoke(fragment, i)
                    if (true == dismissOnClickItem) {
                        fragment.dismiss()
                    }
                }
                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

            return dialog
        }

        private fun initializeItems(
            fragment: AlertDialogFragment,
            builder: AlertDialog.Builder
        ) {
            val labels =
                itemLabelIds?.map { fragment.getText(it) }?.toTypedArray() ?: itemLabels.orEmpty()

            when (itemsMode) {
                ItemsMode.SINGLE_CLICK -> initializeSingleClickItems(builder, labels)
                ItemsMode.SINGLE_CHOICE -> initializeSingleChoiceItems(builder, labels)
                ItemsMode.MULTI_CHOICE -> initializeMultiChoiceItems(builder, labels)
            }
        }

        private fun initializeSingleClickItems(
            builder: AlertDialog.Builder,
            labels: Array<out CharSequence>
        ) {
            builder.setItems(labels, null)
        }

        private fun initializeSingleChoiceItems(
            builder: AlertDialog.Builder,
            labels: Array<out CharSequence>
        ) {
            builder.setSingleChoiceItems(labels, checkedItem, null)
        }

        private fun initializeMultiChoiceItems(
            builder: AlertDialog.Builder,
            labels: Array<out CharSequence>
        ) {
            builder.setMultiChoiceItems(labels, checkedItems, null)
        }
    }

    // ------ //

    /** 項目の表示モード */
    enum class ItemsMode {
        /** 単純に項目を列挙しクリックされたら処理を実行する */
        SINGLE_CLICK,
        /** 項目の中からひとつを選択する */
        SINGLE_CHOICE,
        /** 項目の中から複数を選択する */
        MULTI_CHOICE
    }

    // ------ //

    class Builder(
        private val styleId: Int? = null
    ) {
        private val dialog = AlertDialogFragment.createInstance()
        private val args = dialog.requireArguments().also {
            if (styleId != null) {
                it.putInt(ARG_THEME_ID, styleId)
            }
        }

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

        fun setPositiveButton(@StringRes textId: Int, listener: Listener<AlertDialogFragment>? = null) : Builder {
            args.putInt(ARG_POSITIVE_BUTTON_TEXT_ID, textId)
            dialog.setOnClickPositiveButton(listener)
            return this
        }

        fun setNegativeButton(@StringRes textId: Int, listener: Listener<AlertDialogFragment>? = null) : Builder {
            args.putInt(ARG_NEGATIVE_BUTTON_TEXT_ID, textId)
            dialog.setOnClickNegativeButton(listener)
            return this
        }

        fun setNeutralButton(@StringRes textId: Int, listener: Listener<AlertDialogFragment>? = null) : Builder {
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

        // ------ //

        @Suppress("unchecked_cast")
        inline fun <reified T> setItems(
            labels: List<T>,
            noinline listener: ((dialog: AlertDialogFragment, which: Int)->Unit)? = null
        ) : Builder {
            when (T::class) {
                Int::class -> {
                    setItemsWithLabelIds(labels as List<Int>, listener)
                }

                String::class -> {
                    setItemsWithLabels(labels as List<String>, listener)
                }
            }
            return this
        }

        fun setItemsWithLabelIds(
            labelIds: List<Int>,
            listener: ((dialog: AlertDialogFragment, which: Int)->Unit)? = null
        ) : Builder {
            args.putEnum(ARG_ITEMS_MODE, ItemsMode.SINGLE_CLICK)
            args.putIntArray(ARG_ITEM_LABEL_IDS, labelIds.toIntArray())
            dialog.setOnClickItem(listener)
            return this
        }

        fun setItemsWithLabels(
            labels: List<CharSequence>,
            listener: ((dialog: AlertDialogFragment, which: Int)->Unit)? = null
        ) : Builder {
            args.putEnum(ARG_ITEMS_MODE, ItemsMode.SINGLE_CLICK)
            args.putCharSequenceArray(ARG_ITEM_LABELS, labels.toTypedArray())
            dialog.setOnClickItem(listener)
            return this
        }

        // ------ //

        @Suppress("unchecked_cast")
        inline fun <reified T> setSingleChoiceItems(
            labels: List<T>,
            checkedItem: Int,
            noinline listener: ((dialog: AlertDialogFragment, which: Int)->Unit)? = null
        ) : Builder {
            when (T::class) {
                Int::class -> {
                    setSingleChoiceItemsWithLabelIds(labels as List<Int>, checkedItem, listener)
                }

                String::class -> {
                    setSingleChoiceItemsWithLabels(labels as List<String>, checkedItem, listener)
                }
            }
            return this
        }

        fun setSingleChoiceItemsWithLabelIds(
            labelIds: List<Int>,
            checkedItem: Int,
            listener: ((dialog: AlertDialogFragment, which: Int)->Unit)? = null
        ) : Builder {
            args.putEnum(ARG_ITEMS_MODE, ItemsMode.SINGLE_CHOICE)
            args.putIntArray(ARG_ITEM_LABEL_IDS, labelIds.toIntArray())
            args.putInt(ARG_SINGLE_CHECKED_ITEM, checkedItem)
            dialog.setOnClickItem(listener)
            return this
        }

        fun setSingleChoiceItemsWithLabels(
            labels: List<CharSequence>,
            checkedItem: Int,
            listener: ((dialog: AlertDialogFragment, which: Int)->Unit)? = null
        ) : Builder {
            args.putEnum(ARG_ITEMS_MODE, ItemsMode.SINGLE_CHOICE)
            args.putCharSequenceArray(ARG_ITEM_LABELS, labels.toTypedArray())
            args.putInt(ARG_SINGLE_CHECKED_ITEM, checkedItem)
            dialog.setOnClickItem(listener)
            return this
        }
    }
}


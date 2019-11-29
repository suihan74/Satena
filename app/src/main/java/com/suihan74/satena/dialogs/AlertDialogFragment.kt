package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import java.io.Serializable

interface AlertDialogListener {
    fun onClickPositiveButton(dialog: AlertDialogFragment) {}
    fun onClickNegativeButton(dialog: AlertDialogFragment) {}
    fun onClickNeutralButton(dialog: AlertDialogFragment) {}
    fun onSelectItem(dialog: AlertDialogFragment, which: Int) {}
    fun onSingleSelectItem(dialog: AlertDialogFragment, which: Int) {}
    fun onMultiSelectItem(dialog: AlertDialogFragment, which: Int, selected: Boolean) {}
}

/**
 * 画面復元で落ちないようにしたAlertDialog
 * 独自Viewを表示するDialogFragmentを作成する場合もこのAlertDialogFragmentを継承すると実装が楽
 */
open class AlertDialogFragment : DialogFragment() {
    companion object {
        private const val THEME_RES_ID = "THEME_RES_ID"
        private const val POSITIVE_BUTTON_TEXT_ID = "POSITIVE_BUTTON_TEXT_ID"
        private const val NEGATIVE_BUTTON_TEXT_ID = "NEGATIVE_BUTTON_TEXT_ID"
        private const val NEUTRAL_BUTTON_TEXT_ID = "NEUTRAL_BUTTON_TEXT_ID"
        private const val TITLE_ID = "TITLE_ID"
        private const val TITLE = "TITLE"
        private const val MESSAGE_ID = "MESSAGE_ID"
        private const val MESSAGE = "MESSAGE"
        private const val ICON_ID = "ICON_ID"
        private const val ITEMS = "ITEMS"
        private const val SINGLE_ITEMS_SELECTED = "SINGLE_ITEMS_SELECTED"
        private const val MULTI_ITEMS_STATES = "MULTI_ITEMS_STATES"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (singleChoiceItemPosition != null) {
            outState.putInt(SINGLE_ITEMS_SELECTED, singleChoiceItemPosition!!)
        }

        if (multiChoiceItemsCurrentStates != null) {
            outState.putBooleanArray(MULTI_ITEMS_STATES, multiChoiceItemsCurrentStates!!)
        }
    }

    var items: Array<out String>? = null
        private set

    var singleChoiceItemPosition: Int? = null
        private set

    var multiChoiceItemsCurrentStates: BooleanArray? = null
        private set

    var multiChoiceItemsInitialStates: BooleanArray? = null
        private set

    fun <T> getAdditionalData(key: String) where T : Serializable =
        arguments?.getSerializable(key) as? T

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = createBuilder(arguments!!, savedInstanceState)
        return builder.create()
    }

    protected fun createBuilder(arguments: Bundle, savedInstanceState: Bundle?) : AlertDialog.Builder {
        val themeResId = arguments.getInt(THEME_RES_ID)
        val listener = parentFragment as? AlertDialogListener ?: activity as? AlertDialogListener

        return AlertDialog.Builder(context, themeResId).apply {
            arguments.getInt(TITLE_ID).let {
                if (it != 0) setTitle(it)
            }
            arguments.getCharSequence(TITLE)?.let {
                setTitle(it)
            }
            arguments.getInt(MESSAGE_ID).let {
                if (it != 0) setMessage(it)
            }
            arguments.getCharSequence(MESSAGE)?.let {
                setMessage(it)
            }
            arguments.getInt(ICON_ID).let {
                if (it != 0) setIcon(it)
            }
            arguments.getStringArray(ITEMS)?.let { items ->
                this@AlertDialogFragment.items = items

                val singleSelected = arguments.getInt(SINGLE_ITEMS_SELECTED, -1)
                val multiSelected = arguments.getBooleanArray(MULTI_ITEMS_STATES)

                when {
                    singleSelected >= 0 -> {
                        val savedSelected =
                            savedInstanceState?.getInt(SINGLE_ITEMS_SELECTED, -1) ?: -1
                        val selected = if (savedSelected >= 0) savedSelected else singleSelected
                        setSingleChoiceItems(items, selected) { _, which ->
                            listener?.onSingleSelectItem(this@AlertDialogFragment, which)
                        }
                    }

                    multiSelected != null -> {
                        val states = savedInstanceState?.getBooleanArray(MULTI_ITEMS_STATES)
                            ?: multiSelected.clone()
                        multiChoiceItemsCurrentStates = states
                        multiChoiceItemsInitialStates = multiSelected.clone()

                        setMultiChoiceItems(items, states) { _, which, s ->
                            multiChoiceItemsCurrentStates?.set(which, s)
                            listener?.onMultiSelectItem(this@AlertDialogFragment, which, s)
                        }
                    }

                    else ->
                        setItems(items) { _, which ->
                            listener?.onSelectItem(
                                this@AlertDialogFragment,
                                which
                            )
                        }
                }
            }
            arguments.getInt(POSITIVE_BUTTON_TEXT_ID).let {
                if (it != 0) setPositiveButton(it) { _, _ ->
                    listener?.onClickPositiveButton(this@AlertDialogFragment)
                }
            }
            arguments.getInt(NEGATIVE_BUTTON_TEXT_ID).let {
                if (it != 0) setNegativeButton(it) { _, _ ->
                    listener?.onClickNegativeButton(this@AlertDialogFragment)
                }
            }
            arguments.getInt(NEUTRAL_BUTTON_TEXT_ID).let {
                if (it != 0) setNeutralButton(it) { _, _ ->
                    listener?.onClickNeutralButton(this@AlertDialogFragment)
                }
            }
        }
    }

    open class Builder(themeResId: Int) {
        protected val arguments = Bundle().apply {
            putInt(THEME_RES_ID, themeResId)
        }

        open fun create() =
            AlertDialogFragment().apply {
                this.arguments = this@Builder.arguments
            }

        fun show(fragmentManager: FragmentManager?, tag: String) {
            if (fragmentManager != null) {
                val dialog = create()
                dialog.show(fragmentManager, tag)
            }
        }

        fun setPositiveButton(textId: Int) = this.apply {
            arguments.putInt(POSITIVE_BUTTON_TEXT_ID, textId)
        }

        fun setNegativeButton(textId: Int) = this.apply {
            arguments.putInt(NEGATIVE_BUTTON_TEXT_ID, textId)
        }

        fun setNeutralButton(textId: Int) = this.apply {
            arguments.putInt(NEUTRAL_BUTTON_TEXT_ID, textId)
        }

        fun setTitle(title: CharSequence) = this.apply {
            arguments.remove(TITLE_ID)
            arguments.putCharSequence(TITLE, title)
        }

        fun setTitle(titleId: Int) = this.apply {
            arguments.remove(TITLE)
            arguments.putInt(TITLE_ID, titleId)
        }

        fun setMessage(messageId: Int) = this.apply {
            arguments.remove(MESSAGE)
            arguments.putInt(MESSAGE_ID, messageId)
        }

        fun setMessage(message: CharSequence) = this.apply {
            arguments.remove(MESSAGE_ID)
            arguments.putCharSequence(MESSAGE, message)
        }

        fun setIcon(iconId: Int) = this.apply {
            arguments.putInt(ICON_ID, iconId)
        }

        fun setItems(items: Array<out String>) = this.apply {
            arguments.putStringArray(ITEMS, items)
        }

        fun setItems(items: Collection<String>) =
            setItems(items.toTypedArray())

        fun setSingleChoiceItems(items: Array<out String>, selectedPosition: Int) = this.apply {
            arguments.run {
                remove(MULTI_ITEMS_STATES)
                putStringArray(ITEMS, items)
                putInt(SINGLE_ITEMS_SELECTED, selectedPosition)
            }
        }

        fun setSingleChoiceItems(items: Collection<String>, selectedPosition: Int) =
            setSingleChoiceItems(items.toTypedArray(), selectedPosition)

        fun setMultiChoiceItems(items: Array<out String>, booleanArray: BooleanArray) = this.apply {
            arguments.run {
                remove(SINGLE_ITEMS_SELECTED)
                putStringArray(ITEMS, items)
                putBooleanArray(MULTI_ITEMS_STATES, booleanArray)
            }
        }

        fun setMultiChoiceItems(items: Collection<String>, booleanArray: BooleanArray) =
            setMultiChoiceItems(items.toTypedArray(), booleanArray)

        fun <T> setAdditionalData(key: String, obj: T) where T : Serializable = this.apply {
            arguments.putSerializable(key, obj)
        }
    }
}

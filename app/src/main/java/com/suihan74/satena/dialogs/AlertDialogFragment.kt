package com.suihan74.satena.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.suihan74.utilities.showAllowingStateLoss
import com.suihan74.utilities.getObject
import com.suihan74.utilities.putObject

/**
 * 画面復元で落ちないようにしたAlertDialog
 * 独自Viewを表示するDialogFragmentを作成する場合もこのAlertDialogFragmentを継承すると実装が楽
 */
open class AlertDialogFragment : DialogFragment() {
    companion object {
        private const val KEY_BASE = "AlertDialogFragment."
        const val THEME_RES_ID = KEY_BASE + "THEME_RES_ID"
        const val POSITIVE_BUTTON_TEXT_ID = KEY_BASE + "POSITIVE_BUTTON_TEXT_ID"
        const val NEGATIVE_BUTTON_TEXT_ID = KEY_BASE + "NEGATIVE_BUTTON_TEXT_ID"
        const val NEUTRAL_BUTTON_TEXT_ID = KEY_BASE + "NEUTRAL_BUTTON_TEXT_ID"
        const val TITLE_ID = KEY_BASE + "TITLE_ID"
        const val TITLE = KEY_BASE + "TITLE"
        const val MESSAGE_ID = KEY_BASE + "MESSAGE_ID"
        const val MESSAGE = KEY_BASE + "MESSAGE"
        const val ICON_ID = KEY_BASE + "ICON_ID"
        const val ITEMS = KEY_BASE + "ITEMS"
        const val SINGLE_ITEMS_SELECTED = KEY_BASE + "SINGLE_ITEMS_SELECTED"
        const val MULTI_ITEMS_STATES = KEY_BASE + "MULTI_ITEMS_STATES"
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

    inline fun <reified T> getAdditionalData(key: String) : T? =
        arguments?.getObject<T>(key)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = createBuilder(requireArguments(), savedInstanceState)
        return builder.create()
    }

    protected fun createBuilder(arguments: Bundle, savedInstanceState: Bundle?) : AlertDialog.Builder {
        val themeResId = arguments.getInt(THEME_RES_ID)
        val listener = parentFragment as? Listener ?: activity as? Listener

        return AlertDialog.Builder(requireContext(), themeResId).apply {
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
                            listener?.onSingleChoiceItem(this@AlertDialogFragment, which)
                        }
                    }

                    multiSelected != null -> {
                        val states = savedInstanceState?.getBooleanArray(MULTI_ITEMS_STATES)
                            ?: multiSelected.clone()
                        multiChoiceItemsCurrentStates = states
                        multiChoiceItemsInitialStates = multiSelected.clone()

                        setMultiChoiceItems(items, states) { _, which, s ->
                            multiChoiceItemsCurrentStates?.set(which, s)
                            listener?.onMultiChoiceItem(this@AlertDialogFragment, which, s)
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

        fun show(
            fragmentManager: FragmentManager,
            tag: String? = null
        ) {
            val dialog = create()
            dialog.show(fragmentManager, tag)
        }

        fun showAllowingStateLoss(
            fragmentManager: FragmentManager,
            tag: String? = null,
            onError: ((Throwable)->Unit)? = { Log.e("AlertDialogoBuilder", Log.getStackTraceString(it)) }
        ) {
            val dialog = create()
            dialog.showAllowingStateLoss(fragmentManager, tag, onError)
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

        fun setAdditionalData(key: String, obj: Any?) = this.apply {
            arguments.putObject(key, obj)
        }
    }

    interface Listener {
        fun onClickPositiveButton(dialog: AlertDialogFragment) {}
        fun onClickNegativeButton(dialog: AlertDialogFragment) {}
        fun onClickNeutralButton(dialog: AlertDialogFragment) {}
        fun onSelectItem(dialog: AlertDialogFragment, which: Int) {}
        fun onSingleChoiceItem(dialog: AlertDialogFragment, which: Int) {}
        fun onMultiChoiceItem(dialog: AlertDialogFragment, which: Int, selected: Boolean) {}
    }
}

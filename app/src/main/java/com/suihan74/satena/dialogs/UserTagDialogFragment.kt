package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.utilities.showToast
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class UserTagDialogFragment : AlertDialogFragment(), CoroutineScope {
    private val mJob: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = mJob

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }

    interface Listener: AlertDialogFragment.Listener {
        suspend fun onCompleteEditTagName(tagName: String, dialog: UserTagDialogFragment) : Boolean
    }

    var editingUserTag : Tag? = null
        private set

    val isModifyMode
        get() = editingUserTag != null

    companion object {
        private const val EDITING_USER_TAG = "EDITING_USER_TAG"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_user_tag, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        val listener = parentFragment as? Listener ?: activity as? Listener

        editingUserTag = arguments!!.getSerializable(EDITING_USER_TAG) as? Tag

        val tagName = content.findViewById<EditText>(R.id.tag_name)
        val dialogTitle =
            if (isModifyMode) {
                tagName.setText(editingUserTag!!.name)
                "ユーザータグを編集"
            }
            else {
                "ユーザータグを作成"
            }

        val builder = createBuilder(arguments!!, savedInstanceState).apply {
            setTitle(dialogTitle)
            setView(content)
            setPositiveButton(R.string.dialog_register, null)
            setNegativeButton(R.string.dialog_cancel, null)
        }

        return builder.show().apply {
            getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = tagName.text.toString()

                if (name.isBlank()) {
                    context.showToast("タグ名を入力してください")
                    return@setOnClickListener
                }

                launch(Dispatchers.Main) {
                    if (listener?.onCompleteEditTagName(
                            name,
                            this@UserTagDialogFragment
                        ) != false
                    ) {
                        dismiss()
                    }
                }
            }
        }
    }

    class Builder(themeResId: Int) : AlertDialogFragment.Builder(themeResId) {
        override fun create(): AlertDialogFragment =
            UserTagDialogFragment().apply {
                this.arguments = this@Builder.arguments
            }

        fun setUserTag(tag: Tag) = this.apply {
            arguments.putSerializable(EDITING_USER_TAG, tag)
        }
    }
}

package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
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
        suspend fun onCompletedEditTagName(tagName: String, dialog: UserTagDialogFragment) : Boolean
        suspend fun onAddUserToCreatedTag(tagName: String, user: String, dialog: UserTagDialogFragment) {}
    }

    var editingUserTag : Tag? = null
        private set

    val isModifyMode
        get() = editingUserTag != null

    companion object {
        private const val EDITING_USER_TAG = "EDITING_USER_TAG"
        private const val TARGET_USER = "TARGET_USER"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_user_tag, null)

        val listener = parentFragment as? Listener ?: activity as? Listener

        val arguments = requireArguments()
        editingUserTag = arguments.getSerializable(EDITING_USER_TAG) as? Tag

        val tagName = content.findViewById<EditText>(R.id.tag_name)
        val dialogTitle =
            if (isModifyMode) {
                tagName.setText(editingUserTag!!.name)
                R.string.user_tag_dialog_title_edit_mode
            }
            else {
                R.string.user_tag_dialog_title_create_mode
            }

        val builder = createBuilder(arguments, savedInstanceState).apply {
            setTitle(dialogTitle)
            setView(content)
            setPositiveButton(R.string.dialog_register, null)
            setNegativeButton(R.string.dialog_cancel, null)
        }

        return builder.show().apply {
            getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = tagName.text.toString()

                if (name.isBlank()) {
                    activity?.showToast(R.string.msg_user_tag_no_name)
                    return@setOnClickListener
                }

                launch(Dispatchers.Main) {
                    if (listener?.onCompletedEditTagName(
                            name,
                            this@UserTagDialogFragment
                        ) != false
                    ) {
                        try {
                            val targetUser = arguments.getString(TARGET_USER)
                            if (targetUser != null) {
                                listener?.onAddUserToCreatedTag(
                                    name,
                                    targetUser,
                                    this@UserTagDialogFragment
                                )
                            }
                        }
                        catch (e: Throwable) {
                            Log.e("UserTagDialog", Log.getStackTraceString(e))
                        }

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

        fun setTargetUser(user: String) = this.apply {
            arguments.putString(TARGET_USER, user)
        }
    }
}

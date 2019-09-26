package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.suihan74.satena.R
import com.suihan74.satena.fragments.PreferencesUserTagsFragment
import com.suihan74.satena.models.UserTag
import com.suihan74.utilities.showToast

class UserTagDialogFragment : DialogFragment() {
    private lateinit var mPositiveAction : ((FragmentManager, String, Int)->Boolean)
    private var mEditingUserTag : UserTag? = null
    private val isEditMode
        get() = mEditingUserTag != null

    companion object {
        fun createInstance(positiveAction: ((FragmentManager, String, Int)->Boolean)) = UserTagDialogFragment().apply {
            mPositiveAction = positiveAction
        }

        fun createInstance(tag: UserTag, positiveAction: ((FragmentManager, String, Int)->Boolean)) = UserTagDialogFragment().apply {
            mPositiveAction = positiveAction
            mEditingUserTag = tag
        }

        private const val BUNDLE_EDITING_USER_TAG_NAME = "mEditingUserTag.name"
        private var savedPositiveAction : ((FragmentManager, String, Int)->Boolean)? = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_EDITING_USER_TAG_NAME, mEditingUserTag?.name ?: "")
        savedPositiveAction = mPositiveAction
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_user_tag, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        savedInstanceState?.let {
            val name = it.getString(BUNDLE_EDITING_USER_TAG_NAME)!!
            val parentFragment = parentFragment as PreferencesUserTagsFragment
            mEditingUserTag = parentFragment.userTagsContainer.getTag(name)
        }

        savedPositiveAction?.let {
            mPositiveAction = it
            savedPositiveAction = null
        }

        val tagName = content.findViewById<EditText>(R.id.tag_name)
        val dialogTitle = if (isEditMode) {
            tagName.setText(mEditingUserTag!!.name)
            "ユーザータグを編集"
        }
        else {
            "ユーザータグを作成"
        }


        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setView(content)
            .setMessage(dialogTitle)
            .setPositiveButton("登録", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    if (tagName.text.isNullOrBlank()) {
                        context.showToast("タグ名を入力してください")
                        return@setOnClickListener
                    }

                    if (fragmentManager != null && mPositiveAction.invoke(fragmentManager!!, tagName.text.toString(), 0)) {
                        this.dismiss()
                    }
                }
            }
    }
}

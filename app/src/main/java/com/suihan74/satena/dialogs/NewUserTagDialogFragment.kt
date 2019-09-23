package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.suihan74.satena.R
import com.suihan74.utilities.showToast

class NewUserTagDialogFragment : DialogFragment() {
    private lateinit var mPositiveAction : ((String, Int)->Boolean)
    private var mIsEditMode = false

    companion object {
        fun createInstance(positiveAction: ((String, Int)->Boolean)) = NewUserTagDialogFragment().apply {
            mPositiveAction = positiveAction
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_user_tag, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        val tagName = content.findViewById<EditText>(R.id.tag_name)

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setView(content)
            .setMessage("ユーザータグを作成")
            .setPositiveButton("登録", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    if (tagName.text.isNullOrBlank()) {
                        context.showToast("タグ名を入力してください")
                        return@setOnClickListener
                    }

                    if (mPositiveAction.invoke(tagName.text.toString(), 0)) {
                        this.dismiss()
                    }
                }
            }
    }
}

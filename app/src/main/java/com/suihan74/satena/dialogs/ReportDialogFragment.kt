package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.ReportCategory
import com.suihan74.satena.R
import com.suihan74.utilities.hideSoftInputMethod
import com.suihan74.utilities.showToast
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ReportDialogFragment : DialogFragment(), CoroutineScope {
    private val mJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = mJob

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }

    private var mRoot: View? = null

    private var mEntry: Entry? = null
    private var mBookmark: Bookmark? = null

    /** ユーザー名だけで通報する場合，値が入る */
    private var mUser: String? = null

    companion object {
        fun createInstance(entry: Entry, bookmark: Bookmark) = ReportDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_KEY_ENTRY, entry)
                putSerializable(ARG_KEY_BOOKMARK, bookmark)
            }
        }

        fun createInstance(user: String) = ReportDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_KEY_USER, user)
            }
        }

        private const val ARG_KEY_ENTRY = "mEntry"
        private const val ARG_KEY_BOOKMARK = "mBookmark"
        private const val ARG_KEY_USER = "mUser"

        private const val BUNDLE_CATEGORY = "category"
        private const val BUNDLE_TEXT = "text"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val root = mRoot
        if (root != null) {
            val spinner = root.findViewById<Spinner>(R.id.category_spinner)
            val editText = root.findViewById<EditText>(R.id.text)
            outState.run {
                putInt(BUNDLE_CATEGORY, spinner.selectedItemPosition)
                putString(BUNDLE_TEXT, editText.text.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mEntry = arguments!!.getSerializable(ARG_KEY_ENTRY) as? Entry
        mBookmark = arguments!!.getSerializable(ARG_KEY_BOOKMARK) as? Bookmark
        mUser = arguments!!.getString(ARG_KEY_USER)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_report, null)
        mRoot = content
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        if (mBookmark != null) {
            content.setCustomTitle(mBookmark!!)
        }
        else {
            content.setCustomTitle(mUser!!)
        }

        content.findViewById<EditText>(R.id.text).run {
            setText(
                savedInstanceState?.getString(BUNDLE_TEXT) ?: ""
            )

            // 右端で自動折り返しはするが改行は受け付けない
            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE

            // Doneボタン押下でIME隠す
            setOnEditorActionListener { _, action, _ ->
                when (action) {
                    EditorInfo.IME_ACTION_DONE -> activity?.hideSoftInputMethod() ?: false
                    else -> false
                }
            }
        }

        content.findViewById<Spinner>(R.id.category_spinner).apply {
            adapter = ArrayAdapter(
                context!!,
                R.layout.spinner_report,
                ReportCategory.values().map { it.description }
            ).apply {
                setDropDownViewResource(R.layout.spinner_drop_down_item)
            }

            val initialPosition = savedInstanceState?.getInt(BUNDLE_CATEGORY) ?: 0
            setSelection(initialPosition)
        }

        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setView(content)
            .setMessage("ブコメを通報")
            .setPositiveButton("通報", null)
//            .setNeutralButton("通報して非表示", null)
            .setNegativeButton("Cancel", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    report(mRoot!!, this, false)
                }

                getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                    report(mRoot!!, this, true)
                }
            }
    }

    private fun report(root: View, dialog: AlertDialog, withMuting: Boolean) {
        val spinner = root.findViewById<Spinner>(R.id.category_spinner)
        val editText = root.findViewById<EditText>(R.id.text)

        val category = ReportCategory.values()[spinner.selectedItemPosition]
        val text = editText.text.toString()

        val user = mBookmark?.user ?: mUser ?: throw RuntimeException("invalid user")

        AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setTitle("確認")
            .setIcon(R.drawable.ic_baseline_help)
            .setMessage("id:${user}を「${category.description}」のため通報します。よろしいですか？")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                launch(Dispatchers.Main) {
                    try {
                        when {
                            mEntry != null && mBookmark != null ->
                                HatenaClient.reportAsync(mEntry!!, mBookmark!!, category, text).await()

                            mUser != null ->
                                HatenaClient.reportAsync(mUser!!, category, text).await()

                            else -> throw RuntimeException()
                        }

                        if (withMuting) {
                            HatenaClient.ignoreUserAsync(user).await()
                            activity?.showToast("id:${user}を通報して非表示しました")
                        }
                        else {
                            activity?.showToast("id:${user}を通報しました")
                        }
                    }
                    catch (e: Exception) {
                        activity?.showToast("通報失敗")
                    }
                    finally {
                        dialog.dismiss()
                    }
                }
            }
            .show()
    }
}

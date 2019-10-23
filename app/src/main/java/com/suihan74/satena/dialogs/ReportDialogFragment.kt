package com.suihan74.satena.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.suihan74.HatenaLib.Bookmark
import com.suihan74.HatenaLib.Entry
import com.suihan74.HatenaLib.HatenaClient
import com.suihan74.HatenaLib.ReportCategory
import com.suihan74.satena.R
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
    private lateinit var mEntry: Entry
    private lateinit var mBookmark: Bookmark

    companion object {
        fun createInstance(entry: Entry, bookmark: Bookmark) = ReportDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_KEY_ENTRY, entry)
                putSerializable(ARG_KEY_BOOKMARK, bookmark)
            }
        }

        private const val ARG_KEY_ENTRY = "mEntry"
        private const val ARG_KEY_BOOKMARK = "mBookmark"

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
        mEntry = arguments!!.getSerializable(ARG_KEY_ENTRY) as Entry
        mBookmark = arguments!!.getSerializable(ARG_KEY_BOOKMARK) as Bookmark
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_report, null)
        mRoot = content
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        content.findViewById<TextView>(R.id.user_name).text = mBookmark.user
        content.findViewById<TextView>(R.id.bookmark_comment).text = mBookmark.comment
        val icon = content.findViewById<ImageView>(R.id.user_icon)
        Glide.with(requireContext())
            .load(mBookmark.userIconUrl)
            .into(icon)

        content.findViewById<EditText>(R.id.text).setText(
            savedInstanceState?.getString(BUNDLE_TEXT) ?: ""
        )

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

        launch(Dispatchers.Main) {
            try {
                HatenaClient.reportAsync(mEntry, mBookmark, category, text).await()
                if (withMuting) {
                    HatenaClient.ignoreUserAsync(mBookmark.user).await()
                    activity?.showToast("id:${mBookmark.user}を通報して非表示しました")
                }
                else {
                    activity?.showToast("id:${mBookmark.user}を通報しました")
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
}

package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.ReportCategory
import com.suihan74.satena.R
import com.suihan74.utilities.get
import com.suihan74.utilities.hideSoftInputMethod
import com.suihan74.utilities.showToast
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.fragment_dialog_report.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ReportDialogFragment : DialogFragment(), AlertDialogFragment.Listener {
    private val mEntry : Entry? by lazy {
        requireArguments().getSerializable(ARG_ENTRY) as? Entry
    }
    private val mBookmark: Bookmark? by lazy {
        requireArguments().getSerializable(ARG_BOOKMARK) as? Bookmark
    }

    /** ユーザー名だけで通報する場合，値が入る */
    private val mUser: String? by lazy {
        requireArguments().getString(ARG_USER)
    }

    companion object {
        fun createInstance(entry: Entry, bookmark: Bookmark) = ReportDialogFragment().withArguments {
            putSerializable(ARG_ENTRY, entry)
            putSerializable(ARG_BOOKMARK, bookmark)
        }

        fun createInstance(user: String) = ReportDialogFragment().withArguments {
            putString(ARG_USER, user)
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
        private const val ARG_USER = "ARG_USER"

        private const val BUNDLE_CATEGORY = "category"
        private const val BUNDLE_TEXT = "text"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        view?.let { root ->
            val spinner = root.category_spinner
            val editText = root.text
            outState.run {
                putInt(BUNDLE_CATEGORY, spinner.selectedItemPosition)
                putString(BUNDLE_TEXT, editText.text.toString())
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_report, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        if (mBookmark != null) {
            content.setCustomTitle(mBookmark!!)
        }
        else {
            content.setCustomTitle(mUser!!)
        }

        content.text.run {
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

        content.category_spinner.apply {
            adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_report,
                ReportCategory.values().map { it.description }
            ).apply {
                setDropDownViewResource(R.layout.spinner_drop_down_item)
            }

            val initialPosition = savedInstanceState?.getInt(BUNDLE_CATEGORY) ?: 0
            setSelection(initialPosition)
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setView(content)
            .setMessage(R.string.report_bookmark_dialog_title)
            .setPositiveButton(R.string.report_dialog_ok, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val withMuting = content.ignore_user_after_reporting.isChecked
                    report(content, withMuting)
                }

                getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
                    dismiss()
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().hideSoftInputMethod()
    }

    private fun report(root: View, withMuting: Boolean) {
        val spinner = root.category_spinner
        val editText = root.text

        val category = ReportCategory.values()[spinner.selectedItemPosition]
        val text = editText.text.toString()

        val user = mBookmark?.user ?: mUser ?: throw RuntimeException("invalid user")

        AlertDialogFragment.Builder(R.style.AlertDialogStyle)
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(getString(R.string.report_dialog_confirm_msg, user, category.description))
            .setIcon(R.drawable.ic_baseline_help)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok)
            .setAdditionalData("category", category)
            .setAdditionalData("text", text)
            .setAdditionalData("user", user)
            .setAdditionalData("entry", mEntry)
            .setAdditionalData("bookmark", mBookmark)
            .setAdditionalData("withMuting", withMuting)
            .show(childFragmentManager, "confirm_dialog")
    }

    override fun onClickPositiveButton(dialog: AlertDialogFragment) {
        // reportDialogを消すと確認ダイアログもdetachされるので先に取っておく
        val context = context

        // 以下の処理の待機中に操作可能になってしまうので、先に通報ダイアログを消しておく
        val reportDialog = parentFragmentManager.get<ReportDialogFragment>()
        reportDialog?.dismiss()

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val category = dialog.getAdditionalData<ReportCategory>("category")!!
                val text = dialog.getAdditionalData<String>("text")!!
                val user = dialog.getAdditionalData<String>("user")!!
                val withMuting = dialog.getAdditionalData<Boolean>("withMuting")!!
                val entry = dialog.getAdditionalData<Entry>("entry")
                val bookmark = dialog.getAdditionalData<Bookmark>("bookmark")

                when {
                    entry != null && bookmark != null ->
                        HatenaClient.reportAsync(entry, bookmark, category, text).await()

                    else ->
                        HatenaClient.reportAsync(user, category, text).await()
                }

                if (withMuting) {
                    HatenaClient.ignoreUserAsync(user).await()
                    context?.showToast(R.string.msg_report_and_mute_succeeded, user)
                }
                else {
                    context?.showToast(R.string.msg_report_succeeded, user)
                }
            }
            catch (e: Throwable) {
                context?.showToast(R.string.msg_report_failed)
            }
        }
    }
}

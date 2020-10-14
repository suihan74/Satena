package com.suihan74.satena.scenes.bookmarks2.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.Bookmark
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Report
import com.suihan74.hatenaLib.ReportCategory
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogReport2Binding
import com.suihan74.utilities.SuspendSwitcher
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.launch

class ReportDialog : DialogFragment() {
    companion object {
        fun createInstance(
            entry: Entry,
            bookmark: Bookmark
        ) = ReportDialog().withArguments {
            putObject(ARG_ENTRY, entry)
            putObject(ARG_BOOKMARK, bookmark)
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            val args = requireArguments()
            val entry = args.getObject<Entry>(ARG_ENTRY)!!
            val bookmark = args.getObject<Bookmark>(ARG_BOOKMARK)!!

            DialogViewModel(entry, bookmark)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<FragmentDialogReport2Binding>(
            inflater,
            R.layout.fragment_dialog_report2,
            null,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = parentFragment?.viewLifecycleOwner
            initialize(it)
        }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.report_bookmark_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.report_dialog_ok, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .also { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    lifecycleScope.launch {
                        val result = viewModel.invokeOnReport()
                        if (result) {
                            val user = viewModel.bookmark.user
                            if (viewModel.ignoreAfterReporting.value == true) {
                                context?.showToast(R.string.msg_report_and_ignore_succeeded, user)
                            }
                            else {
                                context?.showToast(R.string.msg_report_succeeded, user)
                            }
                            dialog.dismiss()
                        }
                        else {
                            context?.showToast(R.string.msg_report_failed)
                        }
                    }
                }

                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
                    dialog.dismiss()
                }
            }
    }

    /** コンテンツの初期化 */
    private fun initialize(binding: FragmentDialogReport2Binding) {
        // 通報内容、備考
        binding.text.run {
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

        // 通報カテゴリ
        binding.categorySpinner.apply {
            adapter = ArrayAdapter(
                requireContext(),
                R.layout.spinner_report,
                ReportCategory.values().map { it.description }
            ).apply {
                setDropDownViewResource(R.layout.spinner_drop_down_item)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val category = ReportCategory.fromInt(position)
                        viewModel.category.postValue(category)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        viewModel.category.postValue(null)
                    }
                }
            }

            setSelection(viewModel.category.value?.ordinal ?: 0)
        }
    }

    // ------ //

    /** 送信処理をセット */
    fun setOnReportBookmark(l : SuspendSwitcher<Model>?) = lifecycleScope.launchWhenCreated {
        viewModel.onReport = l
    }

    // ------ //

    data class Model (
        val report: Report,
        val ignoreAfterReporting: Boolean
    )

    // ------ //

    class DialogViewModel(
        val entry: Entry,
        val bookmark: Bookmark
    ) : ViewModel() {

        /** 通報カテゴリ */
        val category =
            MutableLiveData<ReportCategory>(ReportCategory.SPAM)

        /** 通報内容に加えるコメント */
        val comment =
            MutableLiveData<String>("")

        /** 通報後にユーザーを非表示にする */
        val ignoreAfterReporting =
            MutableLiveData<Boolean>()

        val model
            get() = Model(
                Report(
                    entry = entry,
                    bookmark = bookmark,
                    category = category.value ?: ReportCategory.SPAM,
                    comment = comment.value
                ),
                ignoreAfterReporting.value ?: false
            )

        /** 送信時アクション */
        var onReport : SuspendSwitcher<Model>? = null

        suspend fun invokeOnReport() : Boolean {
            return onReport?.invoke(model) ?: true
        }
    }
}

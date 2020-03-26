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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.suihan74.hatenaLib.*
import com.suihan74.satena.R
import com.suihan74.utilities.hideSoftInputMethod
import kotlinx.android.synthetic.main.dialog_title_bookmark.view.*
import kotlinx.android.synthetic.main.fragment_dialog_report.view.*

class ReportDialog : DialogFragment() {
    data class Model (
        val report: Report,
        val ignoreAfterReporting: Boolean
    )

    class ViewModel(
        val entry: Entry,
        val bookmark: Bookmark
    ) : androidx.lifecycle.ViewModel() {

        /** 通報カテゴリ */
        val category by lazy {
            MutableLiveData<ReportCategory>().apply {
                value = ReportCategory.SPAM
            }
        }

        /** 通報内容に加えるコメント */
        val comment by lazy {
            MutableLiveData<String>().apply {
                value = ""
            }
        }

        /** 通報後にユーザーを非表示にする */
        val ignoreAfterReporting by lazy {
            MutableLiveData<Boolean>()
        }

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

        class Factory(
            private val entry: Entry,
            private val bookmark: Bookmark
        ) : ViewModelProvider.NewInstanceFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel?> create(modelClass: Class<T>) =
                ViewModel(entry, bookmark) as T
        }
    }

    private lateinit var viewModel: ViewModel

    companion object {
        fun createInstance(entry: Entry, bookmark: Bookmark) = ReportDialog().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_ENTRY, entry)
                putSerializable(ARG_BOOKMARK, bookmark)
            }
        }

        private const val ARG_ENTRY = "ARG_ENTRY"
        private const val ARG_BOOKMARK = "ARG_BOOKMARK"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val entry = requireArguments().getSerializable(ARG_ENTRY) as Entry
        val bookmark = requireArguments().getSerializable(ARG_BOOKMARK) as Bookmark

        val factory = ViewModel.Factory(entry, bookmark)
        viewModel = ViewModelProvider(this, factory)[ViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content =
            LayoutInflater.from(context)
            .inflate(R.layout.fragment_dialog_report, null)
            .also {
                initialize(it)
            }

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.report_bookmark_dialog_title)
            .setView(content)
            .setPositiveButton(R.string.report_dialog_ok) { _, _ ->
                val listener = parentFragment as? Listener ?: activity as? Listener
                listener?.onReportBookmark(viewModel.model)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().hideSoftInputMethod()
    }

    /** コンテンツの初期化 */
    private fun initialize(content: View) {
        // ブクマ表示
        content.user_name.text = viewModel.bookmark.user
        content.bookmark_comment.text = viewModel.bookmark.comment
        content.user_icon.let {
            Glide.with(requireContext()).run {
                clear(it)
                load(HatenaClient.getUserIconUrl(viewModel.bookmark.user))
                .into(it)
            }
        }

        // 通報内容、備考
        content.text.run {
            setText(viewModel.comment.value)

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

            // テキストの変更をモデルに反映
            addTextChangedListener {
                viewModel.comment.postValue(it.toString())
            }
        }

        // 通報カテゴリ
        content.category_spinner.apply {
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

        // 通報したあと非表示にするかどうか
        content.ignore_user_after_reporting.setOnCheckedChangeListener { _, isChecked ->
            viewModel.ignoreAfterReporting.postValue(isChecked)
        }
    }

    interface Listener {
        fun onReportBookmark(model: Model)
    }
}

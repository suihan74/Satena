package com.suihan74.satena.scenes.bookmarks.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.suihan74.hatenaLib.ReportCategory
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentDialogReportBinding
import com.suihan74.satena.dialogs.createBuilder
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.utilities.SuspendSwitcher
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReportDialog : DialogFragment() {
    companion object {
        fun createInstance(
            user: String,
            userIconUrl: String,
            comment: String? = null
        ) = ReportDialog().withArguments {
            putString(ARG_USER, user)
            putString(ARG_USER_ICON_URL, userIconUrl)
            putString(ARG_COMMENT, comment)
        }

        private const val ARG_USER = "ARG_USER"
        private const val ARG_USER_ICON_URL = "ARG_USER_ICON_URL"
        private const val ARG_COMMENT = "ARG_COMMENT"
    }

    private val viewModel: DialogViewModel by lazy {
        provideViewModel(this) {
            val args = requireArguments()
            val user = args.getString(ARG_USER)!!
            val userIconUrl = args.getString(ARG_USER_ICON_URL)!!
            val comment = args.getString(ARG_COMMENT) ?: ""

            DialogViewModel(user, userIconUrl, comment)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = localLayoutInflater()
        val binding = DataBindingUtil.inflate<FragmentDialogReportBinding>(
            inflater,
            R.layout.fragment_dialog_report,
            null,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = parentFragment?.viewLifecycleOwner
            initialize(it)
        }

        return createBuilder()
            .setTitle(R.string.report_bookmark_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.report_dialog_ok, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
            .also { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    lifecycleScope.launch(Dispatchers.Main) {
                        val result = viewModel.invokeOnReport()
                        if (result) {
                            val user = viewModel.user
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
    private fun initialize(binding: FragmentDialogReportBinding) {
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
        binding.categorySpinner.also { spinner ->
            spinner.adapter =
                ArrayAdapter(
                    requireContext(),
                    R.layout.spinner_report,
                    ReportCategory.values().map { it.description }
                ).also { adapter ->
                    adapter.setDropDownViewResource(R.layout.spinner_drop_down_item)
                }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.category.value = ReportCategory.fromInt(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.category.value = null
                }
            }

            spinner.setSelection(viewModel.category.value?.ordinal ?: 0)
        }
    }

    // ------ //

    /** 送信処理をセット */
    fun setOnReportBookmark(l : SuspendSwitcher<Model>?) = lifecycleScope.launchWhenCreated {
        viewModel.onReport = l
    }

    // ------ //

    data class Model (
        /** 通報対象ユーザー */
        val user : String,

        /** 通報カテゴリ */
        val category : ReportCategory,

        /** 通報内容に加えるコメント */
        val comment : String,

        /** 通報後に非表示にする */
        val ignoreAfterReporting : Boolean
    )

    // ------ //

    class DialogViewModel(
        val user: String,
        val userIconUrl: String,
        val userComment: String
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
                user = user,
                category = category.value ?: ReportCategory.SPAM,
                comment = comment.value ?: "",
                ignoreAfterReporting = ignoreAfterReporting.value ?: false
            )

        /** 送信時アクション */
        var onReport : SuspendSwitcher<Model>? = null

        suspend fun invokeOnReport() : Boolean {
            return onReport?.invoke(model) ?: true
        }
    }
}

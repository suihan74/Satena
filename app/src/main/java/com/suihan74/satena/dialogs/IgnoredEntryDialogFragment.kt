package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.annotation.MainThread
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentDialogIgnoredEntryBinding
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.satena.scenes.preferences.ignored.IgnoredEntriesRepository
import com.suihan74.utilities.DialogListener
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.EmptyException
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.lazyProvideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

class IgnoredEntryDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_EDITING_URL = "ARG_EDITING_URL"
        private const val ARG_EDITING_TEXT = "ARG_EDITING_TEXT"
        private const val ARG_EDIT_MODE = "ARG_EDIT_MODE"
        private const val ARG_MODIFYING_ENTRY = "ARG_MODIFYING_ENTRY"
        private const val ARG_INITIAL_TARGET = "ARG_INITIAL_TARGET"

        fun createInstance(
            url: String = "",
            title: String = ""
        ) = IgnoredEntryDialogFragment().withArguments {
            val editingUrl =
                Regex("""^https?://""").find(url)?.let { r ->
                    url.substring(r.range.last + 1)
                } ?: url

            putString(ARG_EDITING_URL, editingUrl)
            putString(ARG_EDITING_TEXT, title)
            putBoolean(ARG_EDIT_MODE, false)
        }

        fun createInstance(ignoredEntry: IgnoredEntry) = IgnoredEntryDialogFragment().withArguments {
            when (ignoredEntry.type) {
                IgnoredEntryType.URL -> {
                    putString(ARG_EDITING_URL, ignoredEntry.query)
                    putString(ARG_EDITING_TEXT, "")
                }
                IgnoredEntryType.TEXT -> {
                    putString(ARG_EDITING_URL, "")
                    putString(ARG_EDITING_TEXT, ignoredEntry.query)
                }
            }
            putObject(ARG_MODIFYING_ENTRY, ignoredEntry)
            putEnum(ARG_INITIAL_TARGET, ignoredEntry.target) { it.id }
            putBoolean(ARG_EDIT_MODE, ignoredEntry.id >= 0)
        }
    }

    // ------ //

    private val viewModel by lazyProvideViewModel {
        IgnoredEntryDialogViewModel(
            SatenaApplication.instance.ignoredEntriesRepository,
            requireArguments()
        )
    }

    // ------ //

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentDialogIgnoredEntryBinding.inflate(
            localLayoutInflater(),
            null,
            false
        )

        // 最初に表示するタブを選択
        val modifyingEntry = requireArguments().getObject<IgnoredEntry>(ARG_MODIFYING_ENTRY)?.also {
            viewModel.selectTab(it.type)
        } ?: let {
            viewModel.selectedTab.value = viewModel.selectedTab.value ?: IgnoredEntryDialogTab.URL
            null
        }

        val queryText = binding.queryText.apply {
            setText(viewModel.text)
            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE

            addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.text = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        val descText = binding.descText

        // タブが切り替わったときの表示内容更新
        viewModel.selectedTab.observe(this) { tab ->
            queryText.setHint(tab.textId)
            when (tab) {
                IgnoredEntryDialogTab.URL -> {
                    queryText.setText(viewModel.editingUrl.value)
                    descText.setText(R.string.ignored_entry_dialog_desc_url)
                    hideIgnoreTargetArea(binding)
                }
                IgnoredEntryDialogTab.TEXT -> {
                    queryText.setText(viewModel.editingText.value)
                    descText.setText(R.string.ignored_entry_dialog_desc_text)
                    showIgnoreTargetArea(binding)
                }
                null -> {}
            }

            // フォーカスを当てる
            queryText.let {
//                requireActivity().showSoftInputMethod(it)
                it.setSelection(it.text.length)
            }
        }

        binding.tabLayout.apply {
            visibility = (!viewModel.editMode).toVisibility()

            if (viewModel.selectedTab.value != null) {
                val idx = if (viewModel.selectedTab.value == IgnoredEntryDialogTab.URL) 0 else 1
                getTabAt(idx)?.select()
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    viewModel.selectedTab.value = IgnoredEntryDialogTab.fromOrdinal(tab!!.position)
                }
            })
        }

        when (viewModel.selectedTab.value) {
            IgnoredEntryDialogTab.TEXT -> {
                setIgnoreTarget(binding, viewModel.ignoreTarget.value!!)
                showIgnoreTargetArea(binding)
            }
            else -> hideIgnoreTargetArea(binding)
        }

        // 非表示対象を複数チェックボックスを使用して設定する
        val initialIgnoreTarget = modifyingEntry?.target ?: IgnoreTarget.ENTRY
        val onCheckedChange = {
            viewModel.ignoreTarget.value = getIgnoreTarget(binding)
        }
        binding.targetEntryCheckbox.setOnCheckedChangeListener { _, _ -> onCheckedChange() }
        binding.targetBookmarkCheckbox.setOnCheckedChangeListener { _, _ -> onCheckedChange() }
        setIgnoreTarget(binding, initialIgnoreTarget)

        return createBuilder()
            .setTitle(R.string.ignored_entry_dialog_title)
            .setPositiveButton(R.string.dialog_register, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setView(binding.root)
            .show()
            .also { dialog ->
                // IME表示を維持するための設定
                showSoftInputMethod(queryText)

                dialog.getButton(DialogInterface.BUTTON_POSITIVE).let { button ->
                    // (主にキーボード操作の場合)クエリテキストエディタ上でENTER押したらOKボタンにフォーカス移動する
                    queryText.apply {
                        nextFocusForwardId = button.id
                        nextFocusDownId = button.id
                    }

                    button.setOnClickListener {
                        lifecycleScope.launch(Dispatchers.Main) {
                            onClickPositiveButton(modifyingEntry)
                        }
                    }
                }
            }
    }

    /**
     * 「登録」ボタン押下時の処理
     */
    private suspend fun onClickPositiveButton(modifyingEntry: IgnoredEntry?) {
        runCatching {
            viewModel.register(modifyingEntry, this@IgnoredEntryDialogFragment)
        }.onFailure {
            when (it) {
                is EmptyException -> showToast(R.string.msg_ignored_entry_dialog_empty_query)
                is AlreadyExistedException -> showToast(R.string.msg_ignored_entry_dialog_already_existed)
                else -> showToast(R.string.msg_ignored_entry_dialog_failed)
            }
        }.onSuccess { ignoredEntry ->
            showToast(R.string.msg_ignored_entry_dialog_succeeded, ignoredEntry.query)
            runCatching {
                dismiss()
            }
        }
    }

    private fun switchIgnoredTargetAreaVisibility(binding: FragmentDialogIgnoredEntryBinding, visibility: Int) {
        binding.apply {
            targetDescText.visibility = visibility
            targetEntryCheckbox.visibility = visibility
            targetBookmarkCheckbox.visibility = visibility
        }
    }

    private fun showIgnoreTargetArea(binding: FragmentDialogIgnoredEntryBinding) {
        switchIgnoredTargetAreaVisibility(binding, View.VISIBLE)
    }

    private fun hideIgnoreTargetArea(binding: FragmentDialogIgnoredEntryBinding) {
        switchIgnoredTargetAreaVisibility(binding, View.GONE)
    }

    private fun getIgnoreTarget(binding: FragmentDialogIgnoredEntryBinding) : IgnoreTarget {
        val entry = if (binding.targetEntryCheckbox.isChecked) IgnoreTarget.ENTRY else IgnoreTarget.NONE
        val bookmark = if (binding.targetBookmarkCheckbox.isChecked) IgnoreTarget.BOOKMARK else IgnoreTarget.NONE

        return entry or bookmark
    }

    private fun setIgnoreTarget(binding: FragmentDialogIgnoredEntryBinding, target: IgnoreTarget) {
        binding.targetEntryCheckbox.isChecked = target.contains(IgnoreTarget.ENTRY)
        binding.targetBookmarkCheckbox.isChecked = target.contains(IgnoreTarget.BOOKMARK)
    }

    // ------ //

    fun setOnCompleteListener(listener: DialogListener<IgnoredEntry>?) : IgnoredEntryDialogFragment {
        lifecycleScope.launchWhenCreated {
            viewModel.onCompleted = listener
        }
        return this
    }

    // ------ //

    enum class IgnoredEntryDialogTab(
        val textId: Int
    ) {
        URL(R.string.ignored_entry_dialog_tab_url),
        TEXT(R.string.ignored_entry_dialog_tab_text)
        ;

        companion object {
            fun fromOrdinal(idx: Int) = values().getOrElse(idx) { URL }
        }
    }

    // ------ //

    class IgnoredEntryDialogViewModel(
        private val repository: IgnoredEntriesRepository,
        args: Bundle
    ) : ViewModel() {
        /** 編集モードである */
        val editMode: Boolean = args.getBoolean(ARG_EDIT_MODE)

        val editingUrl by lazy {
            MutableLiveData<String>(args.getString(ARG_EDITING_URL).orEmpty())
        }

        val editingText by lazy {
            MutableLiveData<String>(args.getString(ARG_EDITING_TEXT).orEmpty())
        }

        val selectedTab by lazy {
            MutableLiveData<IgnoredEntryDialogTab>()
        }

        val ignoreTarget by lazy {
            MutableLiveData(args.getEnum<IgnoreTarget>(ARG_INITIAL_TARGET) { it.id } ?: IgnoreTarget.ENTRY)
        }

        // ------ //

        /** 登録処理完了後に呼び出すリスナ */
        var onCompleted : DialogListener<IgnoredEntry>? = null

        // ------ //

        init {
            ignoreTarget.value = IgnoreTarget.ENTRY
        }

        // ------ //

        fun createIgnoredEntry(id: Int) = IgnoredEntry(
            type = when (selectedTab.value) {
                IgnoredEntryDialogTab.URL -> IgnoredEntryType.URL
                IgnoredEntryDialogTab.TEXT -> IgnoredEntryType.TEXT
                else -> throw RuntimeException("invalid tab")
            },
            query = text,
            target = ignoreTarget.value!!,
            id = max(id, 0)
        )

        @MainThread
        fun selectTab(ignoredEntryType: IgnoredEntryType) {
            selectedTab.value = when(ignoredEntryType) {
                IgnoredEntryType.URL -> IgnoredEntryDialogTab.URL
                IgnoredEntryType.TEXT -> IgnoredEntryDialogTab.TEXT
            }
        }

        var text: String
            get() = when (selectedTab.value) {
                IgnoredEntryDialogTab.URL -> editingUrl.value!!
                IgnoredEntryDialogTab.TEXT -> editingText.value!!
                else -> throw NullPointerException()
            }
            set(value) = when (selectedTab.value) {
                IgnoredEntryDialogTab.URL -> editingUrl.value = value
                IgnoredEntryDialogTab.TEXT -> editingText.value = value
                else -> Unit
            }

        /**
         * 登録処理
         *
         * @throws EmptyException クエリ文字列が空
         * @throws AlreadyExistedException 重複する設定
         * @throws TaskFailureException 登録処理中に何らかのエラー
         */
        suspend fun register(modifyingEntry: IgnoredEntry?, dialogFragment: IgnoredEntryDialogFragment) : IgnoredEntry {
            if (text.isBlank()) {
                throw EmptyException()
            }

            val ignoredEntry = createIgnoredEntry(modifyingEntry?.id ?: 0)
            val modifyMode = modifyingEntry != null && modifyingEntry.id >= 0

            runCatching {
                if (modifyMode) {
                    repository.updateIgnoredEntry(ignoredEntry)
                }
                else {
                    repository.addIgnoredEntry(ignoredEntry)
                }

                withContext(Dispatchers.Main) {
                    onCompleted?.invoke(ignoredEntry, dialogFragment)
                }
            }.onFailure { e ->
                throw when (e) {
                    is AlreadyExistedException -> e
                    else -> TaskFailureException(cause = e)
                }
            }

            return ignoredEntry
        }
    }
}

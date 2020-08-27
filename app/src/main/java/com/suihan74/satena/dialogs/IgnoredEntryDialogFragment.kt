package com.suihan74.satena.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.ignoredEntry.IgnoreTarget
import com.suihan74.satena.models.ignoredEntry.IgnoredEntry
import com.suihan74.satena.models.ignoredEntry.IgnoredEntryType
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.fragment_dialog_ignored_entry.view.*

enum class IgnoredEntryDialogTab(
    val textId: Int
) {
    URL(R.string.ignored_entry_dialog_tab_url),
    TEXT(R.string.ignored_entry_dialog_tab_text)
    ;

    companion object {
        fun fromInt(i: Int) = values()[i]
    }
}

class IgnoredEntryDialogViewModel : ViewModel() {
    val editingUrl by lazy { MutableLiveData<String>() }
    val editingText by lazy { MutableLiveData<String>() }
    val selectedTab by lazy { MutableLiveData<IgnoredEntryDialogTab>() }
    val ignoreTarget by lazy { MutableLiveData<IgnoreTarget>() }

    init {
        ignoreTarget.value = IgnoreTarget.ENTRY
    }

    fun createIgnoredEntry(id: Int) = IgnoredEntry(
        type = when (selectedTab.value) {
            IgnoredEntryDialogTab.URL -> IgnoredEntryType.URL
            IgnoredEntryDialogTab.TEXT -> IgnoredEntryType.TEXT
            else -> throw RuntimeException("invalid tab")
        },
        query = text,
        target = ignoreTarget.value!!,
        id = id
    )

    fun selectTab(ignoredEntryType: IgnoredEntryType) {
        selectedTab.postValue(
            when(ignoredEntryType) {
                IgnoredEntryType.URL -> IgnoredEntryDialogTab.URL
                IgnoredEntryType.TEXT -> IgnoredEntryDialogTab.TEXT
            }
        )
    }

    var text: String
        get() = when (selectedTab.value) {
            IgnoredEntryDialogTab.URL -> editingUrl.value!!
            IgnoredEntryDialogTab.TEXT -> editingText.value!!
            else -> throw NullPointerException()
        }
        set(value) = when (selectedTab.value) {
            IgnoredEntryDialogTab.URL -> editingUrl.postValue(value)
            IgnoredEntryDialogTab.TEXT -> editingText.postValue(value)
            else -> Unit
        }

    var positiveAction : ((IgnoredEntryDialogFragment, IgnoredEntry)->Boolean)? = null
}

class IgnoredEntryDialogFragment : DialogFragment() {
    var positiveAction : ((IgnoredEntryDialogFragment, IgnoredEntry)->Boolean)? = null

    private lateinit var model: IgnoredEntryDialogViewModel
    private var isEditMode: Boolean = false

    companion object {
        private const val ARG_EDITING_URL = "ARG_EDITING_URL"
        private const val ARG_EDITING_TEXT = "ARG_EDITING_TEXT"
        private const val ARG_EDIT_MODE = "ARG_EDIT_MODE"
        private const val ARG_MODIFYING_ENTRY = "ARG_MODIFYING_ENTRY"
        private const val ARG_INITIAL_TARGET = "ARG_INITIAL_TARGET"

        fun createInstance(
            url: String = "",
            title: String = "",
            positiveAction: ((IgnoredEntryDialogFragment, IgnoredEntry)->Boolean)? = null
        ) = IgnoredEntryDialogFragment().withArguments {
            val editingUrl =
                Regex("""^https?://""").find(url)?.let { r ->
                    url.substring(r.range.last + 1)
                } ?: url

            putString(ARG_EDITING_URL, editingUrl)
            putString(ARG_EDITING_TEXT, title)
            putBoolean(ARG_EDIT_MODE, false)

            it.positiveAction = positiveAction
        }

        fun createInstance(
            ignoredEntry: IgnoredEntry,
            positiveAction: ((IgnoredEntryDialogFragment, IgnoredEntry) -> Boolean)? = null
        ) = IgnoredEntryDialogFragment().withArguments {
            putString(ARG_EDITING_URL, ignoredEntry.query)
            putString(ARG_EDITING_TEXT, ignoredEntry.query)
            putObject(ARG_MODIFYING_ENTRY, ignoredEntry)
            putEnum(ARG_INITIAL_TARGET, ignoredEntry.target) { it.int }
            putBoolean(ARG_EDIT_MODE, true)

            it.positiveAction = positiveAction
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isEditMode = arguments?.getBoolean(ARG_EDIT_MODE) ?: false
        model = ViewModelProvider(this)[IgnoredEntryDialogViewModel::class.java].apply {
            if (savedInstanceState == null) {
                editingUrl.value = arguments?.getString(ARG_EDITING_URL) ?: ""
                editingText.value = arguments?.getString(ARG_EDITING_TEXT) ?: ""
                ignoreTarget.value = arguments?.getEnum<IgnoreTarget>(ARG_INITIAL_TARGET) { it.int } ?: IgnoreTarget.ENTRY
                positiveAction = positiveAction ?: this@IgnoredEntryDialogFragment.positiveAction
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(context)
        val content = inflater.inflate(R.layout.fragment_dialog_ignored_entry, null)
        setStyle(STYLE_NORMAL, R.style.AlertDialogStyle)

        // 最初に表示するタブを選択
        val modifyingEntry = requireArguments().getObject<IgnoredEntry>(ARG_MODIFYING_ENTRY)?.also {
            model.selectTab(it.type)
        } ?: let {
            model.selectedTab.postValue(
                if (isEditMode) model.selectedTab.value
                else IgnoredEntryDialogTab.URL
            )
            null
        }

        val queryText = content.query_text.apply {
            setText(model.editingUrl.value)
            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE

            addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    model.text = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        val descText = content.desc_text

        // タブが切り替わったときの表示内容更新
        // ここで他のフラグメントのようにthisではなくviewLifecycleOwner使うと落ちる
        model.selectedTab.observe(this) { tab ->
            queryText.setHint(tab.textId)
            when (tab) {
                IgnoredEntryDialogTab.URL -> {
                    queryText.setText(model.editingUrl.value)
                    descText.setText(R.string.ignored_entry_dialog_desc_url)
                    hideIgnoreTargetArea(content)
                }
                IgnoredEntryDialogTab.TEXT -> {
                    queryText.setText(model.editingText.value)
                    descText.setText(R.string.ignored_entry_dialog_desc_text)
                    showIgnoreTargetArea(content)
                }
                null -> {}
            }

            // フォーカスを当てる
            queryText.let {
//                requireActivity().showSoftInputMethod(it)
                it.setSelection(it.text.length)
            }
        }

        content.tab_layout.apply {
            visibility = (!isEditMode).toVisibility()

            if (model.selectedTab.value != null) {
                val idx = if (model.selectedTab.value == IgnoredEntryDialogTab.URL) 0 else 1
                getTabAt(idx)?.select()
            }

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    model.selectedTab.value = IgnoredEntryDialogTab.fromInt(tab!!.position)
                }
            })
        }

        when (model.selectedTab.value) {
            IgnoredEntryDialogTab.TEXT -> {
                setIgnoreTarget(content, model.ignoreTarget.value!!)
                showIgnoreTargetArea(content)
            }
            else -> hideIgnoreTargetArea(content)
        }

        // 非表示対象を複数チェックボックスを使用して設定する
        val initialIgnoreTarget = modifyingEntry?.target ?: IgnoreTarget.ENTRY
        val onCheckedChange = {
            model.ignoreTarget.postValue(getIgnoreTarget(content))
        }
        content.target_entry_checkbox.setOnCheckedChangeListener { _, _ -> onCheckedChange() }
        content.target_bookmark_checkbox.setOnCheckedChangeListener { _, _ -> onCheckedChange() }
        setIgnoreTarget(content, initialIgnoreTarget)

        return AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.ignored_entry_dialog_title)
            .setPositiveButton(R.string.dialog_register, null)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setView(content)
            .show()
            .apply {
                // IME表示を維持するための設定
                window?.run {
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
                requireActivity().showSoftInputMethod(queryText, WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

                getButton(DialogInterface.BUTTON_POSITIVE).let {
                    // (主にキーボード操作の場合)クエリテキストエディタ上でENTER押したらOKボタンにフォーカス移動する
                    queryText.apply {
                        nextFocusForwardId = it.id
                        nextFocusDownId = it.id
                    }

                    it.setOnClickListener {
                        if (queryText.text.isNullOrBlank()) {
                            activity?.showToast(R.string.msg_ignored_entry_dialog_empty_query)
                            return@setOnClickListener
                        }

                        val ignoredEntry = model.createIgnoredEntry(modifyingEntry?.id ?: 0)
                        if (model.positiveAction?.invoke(this@IgnoredEntryDialogFragment, ignoredEntry) != false) {
                            dismiss()
                        }
                    }
                }
            }
    }

    private fun showIgnoreTargetArea(root: View) {
        root.apply {
            target_desc_text.visibility = View.VISIBLE
            target_entry_checkbox.visibility = View.VISIBLE
            target_bookmark_checkbox.visibility = View.VISIBLE
        }
    }

    private fun hideIgnoreTargetArea(root: View) {
        root.apply {
            target_desc_text.visibility = View.GONE
            target_entry_checkbox.visibility = View.GONE
            target_bookmark_checkbox.visibility = View.GONE
        }
    }

    private fun getIgnoreTarget(root: View) : IgnoreTarget {
        val entry = if (root.target_entry_checkbox.isChecked) IgnoreTarget.ENTRY else IgnoreTarget.NONE
        val bookmark = if (root.target_bookmark_checkbox.isChecked) IgnoreTarget.BOOKMARK else IgnoreTarget.NONE

        return entry or bookmark
    }

    private fun setIgnoreTarget(root: View, target: IgnoreTarget) {
        root.target_entry_checkbox.isChecked = target.contains(IgnoreTarget.ENTRY)
        root.target_bookmark_checkbox.isChecked = target.contains(IgnoreTarget.BOOKMARK)
    }
}

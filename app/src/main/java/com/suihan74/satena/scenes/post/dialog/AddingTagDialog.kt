package com.suihan74.satena.scenes.post.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.suihan74.satena.databinding.FragmentDialogAddingTagBinding
import com.suihan74.satena.dialogs.localLayoutInflater
import com.suihan74.satena.scenes.post.BookmarkPostViewModelOwner
import com.suihan74.satena.scenes.post.TagsListAdapter
import com.suihan74.utilities.ExpandableBottomSheetDialogFragment
import com.suihan74.utilities.Listener
import com.suihan74.utilities.extensions.showSoftInputMethod

class AddingTagDialog : ExpandableBottomSheetDialogFragment() {

    companion object {
        fun createInstance() = AddingTagDialog()
    }

    // ------ //

    private val bookmarkPostViewModel
        get() = (requireActivity() as BookmarkPostViewModelOwner).bookmarkPostViewModel

    // ------ //

    private var _binding : FragmentDialogAddingTagBinding? = null
    private val binding get() = _binding!!

    override val hiddenTopView: View
        get() = binding.tagsTitleTextView

    override val expandBottomSheetByDefault: Boolean
        get() = bookmarkPostViewModel.expandAddingTagsDialogByDefault

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {
            dismiss()
            return null
        }

        _binding = FragmentDialogAddingTagBinding.inflate(localLayoutInflater(), container, false)

        binding.editText.setOnEditorActionListener { _, i, _ ->
            when (i) {
                EditorInfo.IME_ACTION_DONE -> {
                    complete()
                    true
                }
                else -> false
            }
        }
        dialog?.showSoftInputMethod(requireActivity(), binding.editText)

        binding.addButton.setOnClickListener {
            addTag()
        }

        binding.completeButton.setOnClickListener {
            complete()
        }

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val listener = parentFragment as? OnDismissListener ?: requireActivity() as? OnDismissListener
        listener?.onDismiss(this)
    }

    // ------ //

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        // ダイアログのサイズ変更したときIMEの高さの分だけダイアログがずれるので、IMEを一度閉じて開き直す
        when (newState) {
            BottomSheetBehavior.STATE_EXPANDED,
            BottomSheetBehavior.STATE_COLLAPSED -> {
                val editTextFocused = binding.editText.hasFocus()
                binding.editText.hideSoftInputMethod(binding.root)
                if (editTextFocused) {
                    dialog?.showSoftInputMethod(requireActivity(), binding.editText)
                }
            }
            else -> {}
        }

        // 最初の全画面化時に既存のタグリストをロードする
        // onCreate時に表示しているとボトムシート表示時に一瞬チラつくため
        if (newState == BottomSheetBehavior.STATE_EXPANDED && binding.tagsList.adapter == null) {
            initializeTagsList()
        }
    }

    // ------ //

    /** ユーザーのタグリストを用意する */
    private fun initializeTagsList() {
        binding.tagsList.apply {
            layoutManager = ChipsLayoutManager.newBuilder(requireContext())
                .setRowStrategy(ChipsLayoutManager.STRATEGY_DEFAULT)
                .build()

            adapter = TagsListAdapter().also { adapter ->
                bookmarkPostViewModel.repository.tags.observe(viewLifecycleOwner) { tags ->
                    adapter.setTags(tags.map { t -> t.text })
                }
                adapter.setOnItemClickedListener { tag ->
                    binding.editText.text?.let { editable ->
                        editable.clear()
                        editable.append(tag)
                    }
                }
            }
        }
    }

    // ------ //

    private var onAddingTagListener : Listener<String>? = null

    fun setOnAddingTagListener(l : Listener<String>) : AddingTagDialog {
        onAddingTagListener = l
        return this
    }

    private fun complete() {
        addTag()
        dismiss()
    }

    private fun addTag() {
        binding.editText.text?.toString().orEmpty().let { tag ->
            runCatching {
                onAddingTagListener?.invoke(tag)
            }
            binding.editText.text?.clear()
        }
    }

    // ------ //

    interface OnDismissListener {
        fun onDismiss(dialog: AddingTagDialog)
    }
}

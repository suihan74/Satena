package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.activity_entries2.*

class MyBookmarksEntriesFragment : TwinTabsEntriesFragment() {
    companion object {
        fun createInstance() = MyBookmarksEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.MyBookmarks)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = MyBookmarksViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, MyBookmarksViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        // タグを選択している場合、戻るボタンでタグ選択を解除する
        val clearTagCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.tag.value != null) {
                    viewModel.tag.value = null
                }
            }
        }

        // タグ選択時にサブタイトルを表示する
        val toolbar = requireActivity().toolbar
        viewModel.tag.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = it?.let { tag -> "${tag.text}(${tag.count})" }

            // 戻るボタンの処理に割り込む
            clearTagCallback.isEnabled = it != null
        })

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, clearTagCallback)

        setHasOptionsMenu(true)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val viewModel = viewModel as MyBookmarksViewModel

        inflater.inflate(R.menu.spinner_issues, menu)

        viewModel.tags.observe(viewLifecycleOwner, Observer { tags ->
            val activity = requireActivity()

            // タグ選択
            (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
                val spinnerItems = tags.map { "${it.text}(${it.count})" }
                initialize(
                    activity,
                    spinnerItems,
                    R.drawable.spinner_allow_tags,
                    getString(R.string.desc_tags_spinner)
                ) { position ->
                    val tag =
                        if (position == null) null
                        else tags[position]

                    viewModel.tag.value = tag
                }

                if (viewModel.tag.value != null) {
                    val currentIssueName = viewModel.tag.value?.text
                    val position = tags.indexOfFirst { it.text == currentIssueName }
                    if (position >= 0) {
                        setSelection(position + 1)
                    }
                }
            }
        })
    }
}

package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
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

        // タグ選択時にサブタイトルを表示する
        val toolbar = requireActivity().toolbar
        viewModel.tag.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = it?.let { tag -> "${tag.text}(${tag.count})" }
        })

        setHasOptionsMenu(true)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val viewModel = viewModel as MyBookmarksViewModel

        var inflated = false
        viewModel.tags.observe(viewLifecycleOwner, Observer { tags ->
            val activity = requireActivity()

            if (!inflated) {
                inflater.inflate(R.menu.spinner_issues, menu)
                inflated = true
            }

            (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
                val spinnerItems = tags.map { "${it.text}(${it.count})" }
                initialize(
                    activity,
                    spinnerItems,
                    R.drawable.spinner_allow_tags,
                    getString(R.string.desc_issues_spinner)
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

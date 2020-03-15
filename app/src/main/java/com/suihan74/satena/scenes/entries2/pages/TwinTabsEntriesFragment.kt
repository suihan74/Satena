package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentEntries2Binding
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesFragment
import com.suihan74.satena.scenes.entries2.EntriesTabAdapter
import com.suihan74.satena.scenes.entries2.EntriesTabFragment
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.fragment_entries2.view.*

class TwinTabsEntriesFragment : EntriesFragment() {
    companion object {
        fun createInstance(category: Category) = TwinTabsEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, category)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val binding = DataBindingUtil.inflate<FragmentEntries2Binding>(inflater, R.layout.fragment_entries2, container, false).apply {
            lifecycleOwner = this@TwinTabsEntriesFragment
            vm = viewModel
        }

        val view = binding.root

        // タブ設定
        view.entries_tab_pager.adapter =
            EntriesTabAdapter(this)
        view.main_tab_layout.apply {
            setupWithViewPager(view.entries_tab_pager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {}
                override fun onTabUnselected(p0: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    val adapter = view.entries_tab_pager.adapter as EntriesTabAdapter
                    val position = tab!!.position
                    val fragment = adapter.instantiateItem(view.entries_tab_pager, position) as? EntriesTabFragment
                    fragment?.scrollToTop()
                }
            })
        }

        setHasOptionsMenu(category == Category.MyBookmarks || category.hasIssues)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        when (category) {
            Category.MyBookmarks -> inflateMyBookmarksMenu(menu, inflater)

            else -> {
                viewModel.issues.observe(viewLifecycleOwner, Observer { issues ->
                    if (issues != null) {
                        inflateIssuesMenu(menu, inflater, issues)
                    }
                })
            }
        }
    }

    /** マイブックマーク画面用のメニュー */
    private fun inflateMyBookmarksMenu(menu: Menu, inflater: MenuInflater) {
        val activity = requireActivity()
        inflater.inflate(R.menu.spinner_issues, menu)
        (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
            initialize(activity, emptyList(), R.drawable.spinner_allow_tags, getString(R.string.desc_issues_spinner)) { position ->
                // TODO: Tagスピナーの挙動
            }
        }
    }

    /** カテゴリごとの特集を選択する追加メニュー */
    private fun inflateIssuesMenu(menu: Menu, inflater: MenuInflater, issues: List<Issue>) {
        val activity = requireActivity() as EntriesActivity
        val spinnerItems = issues.map { it.name }

        inflater.inflate(R.menu.spinner_issues, menu)

        (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
            initialize(activity, spinnerItems, R.drawable.spinner_allow_issues, getString(R.string.desc_issues_spinner)) { position ->
                viewModel.issue.value =
                    if (position == null) null
                    else {
                        val item = spinnerItems[position]
                        issues.firstOrNull { it.name == item }
                    }
            }

            if (viewModel.issue.value != null) {
                val currentIssueName = viewModel.issue.value?.name
                val position = spinnerItems.indexOfFirst { it == currentIssueName }
                if (position >= 0) {
                    setSelection(position + 1)
                }
            }
        }
    }
}

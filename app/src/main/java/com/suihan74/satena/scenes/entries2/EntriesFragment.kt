package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import kotlinx.android.synthetic.main.activity_entries2.*

abstract class EntriesFragment : Fragment() {
    /** EntriesActivityのViewModel */
    protected lateinit var activityViewModel : EntriesViewModel

    protected lateinit var viewModel : EntriesFragmentViewModel

    /** タブタイトルを取得する */
    abstract fun getTabTitleId(position: Int) : Int
    abstract val tabCount : Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProviders.of(requireActivity())[EntriesViewModel::class.java]
        val category = activityViewModel.currentCategory.value

        viewModel = ViewModelProviders.of(this)[EntriesFragmentViewModel::class.java]
        viewModel.category.value = category
        setHasOptionsMenu(category == Category.MyBookmarks || category?.hasIssues == true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activityViewModel.currentCategory.value = viewModel.category.value

        val toolbar = requireActivity().toolbar.apply {
            setTitle(viewModel.category.value?.textId ?: 0)
            subtitle = viewModel.issue.value?.name
        }

        viewModel.category.observe(viewLifecycleOwner, Observer {
            toolbar.setTitle(it.textId)
        })

        viewModel.issue.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = it?.name
        })

        return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        viewModel.issues.observe(viewLifecycleOwner, Observer { issues ->
            if (issues != null) {
                inflateIssuesMenu(menu, inflater, issues)
            }
        })
    }

    /** カテゴリごとの特集を選択する追加メニュー */
    private fun inflateIssuesMenu(menu: Menu, inflater: MenuInflater, issues: List<Issue>) {
        val activity = requireActivity() as EntriesActivity
        val spinnerItems = issues.map { it.name }

        inflater.inflate(R.menu.spinner_issues, menu)

        (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
            initialize(activity, spinnerItems, R.drawable.spinner_allow_issues, getString(R.string.desc_issues_spinner)) { position ->
                val prevIssue = viewModel.issue.value
                viewModel.issue.value =
                    if (position == null) null
                    else {
                        val item = spinnerItems[position]
                        issues.firstOrNull { it.name == item }
                    }

                if (prevIssue != viewModel.issue.value) {
//                        refreshEntriesTabs(currentCategory!!)
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

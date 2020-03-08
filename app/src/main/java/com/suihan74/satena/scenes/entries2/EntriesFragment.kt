package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import com.suihan74.satena.scenes.entries2.pages.HatenaEntriesViewModel
import com.suihan74.satena.scenes.entries2.pages.MyBookmarksViewModel
import kotlinx.android.synthetic.main.activity_entries2.*

abstract class EntriesFragment : Fragment() {
    companion object {
        @JvmStatic
        protected val ARG_CATEGORY = "ARG_CATEGORY"
    }

    /** EntriesActivityのViewModel */
    protected lateinit var activityViewModel : EntriesViewModel

    protected lateinit var viewModel : EntriesFragmentViewModel

    /** この画面のCategory */
    val category : Category
        get() = viewModel.category.value!!

    /** この画面のIssue */
    val issue : Issue?
        get() = viewModel.issue.value

    /** タブタイトルを取得する */
    fun getTabTitleId(position: Int) = viewModel.getTabTitleId(position)
    val tabCount get() = viewModel.tabCount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityViewModel = ViewModelProvider(requireActivity())[EntriesViewModel::class.java]
        val category = Category.fromInt(requireArguments().getInt(ARG_CATEGORY))

        val viewModelType =
            if (category == Category.MyBookmarks) MyBookmarksViewModel::class.java
            else HatenaEntriesViewModel::class.java

        viewModel = ViewModelProvider(this)[viewModelType]
        viewModel.category.value = category
        setHasOptionsMenu(category == Category.MyBookmarks || category.hasIssues)

        // 画面遷移時にフェードする
        enterTransition = TransitionSet().addTransition(Fade())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ツールバーを更新
        requireActivity().toolbar.apply {
            setTitle(viewModel.category.value?.textId ?: 0)
            subtitle = viewModel.issue.value?.name
        }
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

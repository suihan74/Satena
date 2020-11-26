package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.extensions.alsoAs
import com.suihan74.utilities.extensions.putEnum
import com.suihan74.utilities.extensions.withArguments
import com.suihan74.utilities.provideViewModel

class HatenaEntriesFragment : MultipleTabsEntriesFragment() {
    companion object {
        fun createInstance(category: Category) = HatenaEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, category)
        }
    }

    private var clearIssueCallback : OnBackPressedCallback? = null

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ) = provideViewModel(owner, viewModelKey) {
        HatenaEntriesViewModel(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        // ツールバーを更新
        activity.alsoAs<EntriesActivity> { activity ->
            activity.toolbar.also {
                it.title = getString(category.textId)
                it.subtitle = viewModel.issue.value?.name
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()

        activity.alsoAs<EntriesActivity> {
            setHasOptionsMenu(!it.viewModel.isBottomLayoutMode && category.hasIssues)
        }
    }

    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ): Boolean {
        val result = super.updateActivityAppBar(activity, tabLayout, bottomAppBar)

        bottomAppBar?.let { appBar ->
            activity.inflateExtraBottomMenu(R.menu.spinner_issues_bottom)
            initializeMenu(appBar.menu)
        }

        return result
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.spinner_issues, menu)
        initializeMenu(menu)
    }

    /** サブカテゴリ選択ボックスを初期化する */
    private fun initializeMenu(menu: Menu) {
        val menuItem = menu.findItem(R.id.issues_spinner)
        val spinner = menuItem?.actionView as? Spinner ?: return

        // ロード完了まで隠しておく
        spinner.visibility = View.GONE

        val viewModel = viewModel as HatenaEntriesViewModel

        viewModel.issues.observe(viewLifecycleOwner) { issues ->
            val activity = requireActivity() as EntriesActivity
            val spinnerItems = issues.map { it.name }

            if (issues.isNotEmpty()) {
                spinner.visibility = View.VISIBLE
            }

            spinner.run {
                initialize(
                    activity,
                    menuItem,
                    spinnerItems,
                    R.string.desc_issues_spinner
                ) { position ->
                    viewModel.issue.value =
                        if (position == null) null
                        else issues[position]
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

        // Issue選択時にサブタイトルを表示する
        viewModel.issue.observe(viewLifecycleOwner) {
            val toolbar = (requireActivity() as EntriesActivity).toolbar
            toolbar.subtitle = it?.name

            clearIssueCallback?.isEnabled = it != null

            if (it == null) {
                spinner.setSelection(0)
            }
        }

        // Issueを選択している場合、戻るボタンで選択を解除する
        clearIssueCallback?.remove()
        clearIssueCallback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, viewModel.issue.value != null) {
            if (viewModel.issue.value != null) {
                viewModel.issue.value = null
            }
        }
    }
}

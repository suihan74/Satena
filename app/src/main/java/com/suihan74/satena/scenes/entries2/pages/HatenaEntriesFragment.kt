package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.activity_entries2.*

class HatenaEntriesFragment : TwinTabsEntriesFragment() {
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
    ): EntriesFragmentViewModel {
        val factory = HatenaEntriesViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, HatenaEntriesViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)
        val toolbar = requireActivity().toolbar

        // Issueを選択している場合、戻るボタンで選択を解除する
        clearIssueCallback?.remove()
        clearIssueCallback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, viewModel.issue.value != null) {
            if (viewModel.issue.value != null) {
                viewModel.issue.value = null
            }
        }

        // ツールバーを更新
        toolbar.also {
            it.title = getString(category.textId)
            it.subtitle = viewModel.issue.value?.name
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(category.hasIssues)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val viewModel = viewModel as HatenaEntriesViewModel

        inflater.inflate(R.menu.spinner_issues, menu)

        val spinner = (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.apply {
            visibility = View.GONE
        }

        viewModel.issues.observe(viewLifecycleOwner) { issues ->
            val activity = requireActivity() as EntriesActivity
            val spinnerItems = issues.map { it.name }

            if (issues.isNotEmpty()) {
                spinner?.visibility = View.VISIBLE
            }

            spinner?.run {
                initialize(
                    activity,
                    spinnerItems,
                    R.drawable.spinner_allow_issues,
                    getString(R.string.desc_issues_spinner)
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
            val toolbar = requireActivity().toolbar
            toolbar.subtitle = it?.name

            clearIssueCallback?.isEnabled = it != null

            if (it == null) {
                spinner?.setSelection(0)
            }
        }
    }
}

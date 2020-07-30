package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
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
        val clearIssueCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.issue.value != null) {
                    viewModel.issue.value = null
                }
            }
        }

        // ツールバーを更新
        toolbar.also {
            it.title = getString(category.textId)
            it.subtitle = viewModel.issue.value?.name
        }

        // Issue選択時にサブタイトルを表示する
        viewModel.issue.observe(viewLifecycleOwner) {
            toolbar.subtitle = it?.name

            clearIssueCallback.isEnabled = it != null
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, clearIssueCallback)

        return root
    }

    override fun onResume() {
        super.onResume()
        setHasOptionsMenu(category.hasIssues)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val viewModel = viewModel as HatenaEntriesViewModel

        var inflated = false
        viewModel.issues.observe(viewLifecycleOwner) { issues ->
            if (issues == null) return@observe

            val activity = requireActivity() as EntriesActivity
            val spinnerItems = issues.map { it.name }

            if (!inflated) {
                inflater.inflate(R.menu.spinner_issues, menu)
                inflated = true
            }
            (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
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
    }
}

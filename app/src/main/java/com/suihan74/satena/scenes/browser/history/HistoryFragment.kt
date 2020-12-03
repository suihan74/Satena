package com.suihan74.satena.scenes.browser.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserHistoryBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showSoftInputMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryFragment :
    Fragment(),
    ScrollableToTop,
    TabItem
{
    companion object {
        fun createInstance() = HistoryFragment()
    }

    private val browserActivity : BrowserActivity
        get() = requireActivity() as BrowserActivity

    private val activityViewModel : BrowserViewModel
        get() = browserActivity.viewModel

    private val viewModel by lazyProvideViewModel {
        HistoryViewModel(activityViewModel.historyRepo)
    }

    private var binding: FragmentBrowserHistoryBinding? = null

    private var onBackPressedCallback : OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentBrowserHistoryBinding>(
            inflater,
            R.layout.fragment_browser_history,
            container,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }
        this.binding = binding

        binding.recyclerView.let { recyclerView ->
            recyclerView.setHasFixedSize(true)
            recyclerView.adapter = HistoryAdapter(viewModel, viewLifecycleOwner).also { adapter ->
                adapter.setOnClickItemListener { binding ->
                    val history = binding.history ?: return@setOnClickItemListener
                    viewModel.goAddress(history.page.url, browserActivity)
                }

                adapter.setOnLongLickItemListener { binding ->
                    val site = binding.history ?: return@setOnLongLickItemListener
                    viewModel.openItemMenuDialog(site, browserActivity, childFragmentManager)
                }

                // 日付指定で履歴を一括削除する
                adapter.setOnClearByDateListener { date ->
                    viewModel.openClearByDateDialog(date, childFragmentManager)
                }
            }
            // スクロールで続きを取得
            recyclerView.addOnScrollListener(
                RecyclerViewScrollingUpdater {
                    lifecycleScope.launch(Dispatchers.Main) {
                        kotlin.runCatching {
                            viewModel.loadAdditional()
                        }
                        loadCompleted()
                    }
                }
            )
        }

        // 検索ボックスの表示切替
        binding.searchButton.setOnClickListener {
            viewModel.keywordEditTextVisible.value = !binding.searchText.isVisible
        }

        // 検索ボックスの表示状態にあわせてIMEの表示を切り替える
        viewModel.keywordEditTextVisible.observe(viewLifecycleOwner) {
            if (it) {
                browserActivity.showSoftInputMethod(binding.searchText)
                enableOnBackPressedCallback()
            }
            else {
                browserActivity.hideSoftInputMethod(binding.mainLayout)
                disableOnBackPressedCallback()
            }
        }

        // 入力完了でIMEを閉じる
        binding.searchText.setOnEditorActionListener { _, action, _ ->
            when (action) {
                EditorInfo.IME_ACTION_DONE -> {
                    browserActivity.hideSoftInputMethod(binding.mainLayout)
                    true
                }

                else -> false
            }
        }

        return binding.root
    }

    /** 戻るボタンで検索ボックスを閉じる */
    private fun enableOnBackPressedCallback() {
        onBackPressedCallback = browserActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.keywordEditTextVisible.value = false
            viewModel.keyword.value = ""
            disableOnBackPressedCallback()
        }
    }

    private fun disableOnBackPressedCallback() {
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
    }

    // ------ //

    override fun scrollToTop() {
        binding?.recyclerView?.scrollToPosition(0)
    }

    // ------ //

    override fun onTabSelected() {}

    override fun onTabUnselected() {
        onBackPressedCallback?.handleOnBackPressed()
    }

    override fun onTabReselected() {
    }
}

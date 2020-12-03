package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesBrowserBinding
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.BrowserViewModel
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.browser.UrlBlockingFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.TabItem
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.hideSoftInputMethod
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.lazyProvideViewModel

class PreferencesBrowserFragment :
    Fragment(),
    ScrollableToTop,
    TabItem
{
    companion object {
        fun createInstance() = PreferencesBrowserFragment()
    }

    private val preferencesActivity : PreferencesActivity?
        get() = requireActivity() as? PreferencesActivity

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    private val browserViewModel : BrowserViewModel?
        get() = browserActivity?.viewModel

    private lateinit var binding: FragmentPreferencesBrowserBinding

    private var onBackPressedCallback : OnBackPressedCallback? = null

    val viewModel: PreferencesBrowserViewModel by lazyProvideViewModel {
        val context = requireContext()

        // ブラウザから直接開かれている場合はリポジトリを共有して変更をすぐに反映させる
        val browserRepo = browserViewModel?.browserRepo ?:
            BrowserRepository(
                HatenaClient,
                SafeSharedPreferences.create<PreferenceKey>(context),
                SafeSharedPreferences.create<BrowserSettingsKey>(context)
            )

        val historyRepo = browserViewModel?.historyRepo ?:
            HistoryRepository(SatenaApplication.instance.browserDao)

        PreferencesBrowserViewModel(
            browserRepo,
            historyRepo,
            isPreferencesActivity = preferencesActivity != null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate<FragmentPreferencesBrowserBinding>(
            inflater,
            R.layout.fragment_preferences_browser,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        // ブラウザ画面から開かれている場合、
        // 閲覧中のページのアドレスを入力する
        browserViewModel?.url?.observe(viewLifecycleOwner) {
            viewModel.startPageEditText.value = it
        }

        binding.startPageEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                requireActivity().hideSoftInputMethod(binding.contentLayout)
            }
        }

        binding.registerStartPageButton.setOnClickListener {
            try {
                viewModel.registerStartPageUrl()
                context?.showToast(R.string.msg_browser_updating_start_page_succeeded)
            }
            catch (e: InvalidUrlException) {
                context?.showToast(R.string.msg_browser_updating_start_page_failed)
            }
        }

        binding.userAgentEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                requireActivity().hideSoftInputMethod(binding.contentLayout)
            }
        }

        binding.webViewThemeButton.setOnClickListener {
            viewModel.openWebViewThemeSelectionDialog(childFragmentManager)
        }

        binding.browserModeButton.setOnClickListener {
            viewModel.openBrowserModeSelectionDialog(childFragmentManager)
        }

        // URLブロック設定を編集する画面を開く
        binding.openBlockingUrlsButton.setOnClickListener {
            val fragment = UrlBlockingFragment.createInstance()
            childFragmentManager.beginTransaction()
                .replace(R.id.child_fragment_layout, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss()

            enableOnBackPressedCallback()
        }

        // 検索エンジンを設定するダイアログを開く
        binding.searchEngineSelectionButton.setOnClickListener {
            viewModel.openSearchEngineSelectionDialog(childFragmentManager)
        }

        // キャッシュ削除
        binding.webViewClearCacheButton.setOnClickListener {
            viewModel.openClearCacheDialog(childFragmentManager)
        }

        // クッキー削除
        binding.webViewClearCookieButton.setOnClickListener {
            viewModel.openClearCookieDialog(childFragmentManager)
        }

        // 閲覧履歴削除
        binding.webViewClearHistoryButton.setOnClickListener {
            viewModel.openClearHistoryDialog(childFragmentManager)
        }

        // 画面復元時に戻るボタンの割り込みを設定し直す
        if (childFragmentManager.findFragmentById(R.id.child_fragment_layout) != null
            && onBackPressedCallback == null
        ) {
            enableOnBackPressedCallback()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // 設定画面では、他のタブが生成したオプションメニューがあったら消す
        if (preferencesActivity != null) {
            setHasOptionsMenu(false)
        }
    }

    private fun enableOnBackPressedCallback() {
        onBackPressedCallback = activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
            childFragmentManager.popBackStack()
            onBackPressedCallback = null
            this.remove()
        }
    }

    // ------ //

    override fun scrollToTop() {
        binding.scrollView.scrollTo(0, 0)
    }

    // ------ //

    override fun onTabSelected() {}

    override fun onTabUnselected() {
        onBackPressedCallback?.handleOnBackPressed()
    }

    override fun onTabReselected() {}
}

package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.databinding.FragmentPreferencesBrowserBinding
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.browser.*
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.utilities.*
import com.suihan74.utilities.extensions.hideSoftInputMethod

class PreferencesBrowserFragment : Fragment() {
    companion object {
        fun createInstance() = PreferencesBrowserFragment()
    }

    private val preferencesActivity : PreferencesActivity?
        get() = requireActivity() as? PreferencesActivity

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    private val browserViewModel : BrowserViewModel?
        get() = browserActivity?.viewModel

    private val viewModel: PreferencesBrowserViewModel by lazy {
        // ブラウザから直接開かれている場合はリポジトリを共有して変更をすぐに反映させる
        provideViewModel(this) {
            val context = requireContext()
            val repository = browserViewModel?.repository ?:
                BrowserRepository(
                    HatenaClient,
                    AccountLoader(
                        context,
                        HatenaClient,
                        MastodonClientHolder
                    ),
                    SafeSharedPreferences.create<PreferenceKey>(context),
                    SafeSharedPreferences.create<BrowserSettingsKey>(context),
                    SatenaApplication.instance.browserDao
                )

            PreferencesBrowserViewModel(repository).also {
                it.isPreferencesActivity = preferencesActivity != null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentPreferencesBrowserBinding>(
            inflater,
            R.layout.fragment_preferences_browser,
            container,
            false
        ).apply {
            vm = viewModel
            lifecycleOwner = viewLifecycleOwner
        }

        binding.startPageEditText.setOnFocusChangeListener { view, b ->
            if (!b) {
                requireActivity().hideSoftInputMethod(binding.contentLayout)
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

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // 設定画面では、他のタブが生成したオプションメニューがあったら消す
        if (preferencesActivity != null) {
            setHasOptionsMenu(false)
        }
    }
}

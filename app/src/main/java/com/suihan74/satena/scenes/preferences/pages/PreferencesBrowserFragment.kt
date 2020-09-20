package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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

    private val DIALOG_WEB_VIEW_THEME by lazy { "DIALOG_WEB_VIEW_THEME" }
    private val DIALOG_BROWSER_MODE by lazy { "DIALOG_BROWSER_MODE" }
    private val DIALOG_CLEAR_CACHE by lazy { "DIALOG_CLEAR_CACHE" }
    private val DIALOG_CLEAR_COOKIE by lazy { "DIALOG_CLEAR_COOKIE" }
    private val DIALOG_CLEAR_HISTORY by lazy { "DIALOG_CLEAR_HISTORY" }

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
                    SafeSharedPreferences.create<BrowserSettingsKey>(context)
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

        @OptIn(ExperimentalStdlibApi::class)
        binding.webViewThemeButton.setOnClickListener {
            val items = buildList {
                add(WebViewTheme.AUTO)
                add(WebViewTheme.NORMAL)
                if (viewModel.isForceDarkSupported) {
                    if (viewModel.isForceDarkStrategySupported) {
                        add(WebViewTheme.DARK)
                    }
                    add(WebViewTheme.FORCE_DARK)
                }
            }
            val labels = items.map { it.textId }

            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.pref_browser_theme_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setItems(labels) { _, which ->
                    viewModel.webViewTheme.value = items[which]
                }
                .create()

            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_WEB_VIEW_THEME)
        }

        binding.browserModeButton.setOnClickListener {
            val labels = BrowserMode.values().map { it.textId }
            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.pref_browser_mode_desc)
                .setNegativeButton(R.string.dialog_cancel)
                .setItems(labels) { _, which ->
                    viewModel.browserMode.value = BrowserMode.fromOrdinal(which)
                }
                .create()

            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_BROWSER_MODE)
        }

        // キャッシュ削除
        binding.webViewClearCacheButton.setOnClickListener {
            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(R.string.pref_browser_clear_cache_dialog_message)
                .setNegativeButton(R.string.dialog_cancel)
                .setPositiveButton(R.string.dialog_ok) {
                    WebView(SatenaApplication.instance).clearCache(true)
                }
                .create()
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_CLEAR_CACHE)
        }

        // クッキー削除
        binding.webViewClearCookieButton.setOnClickListener {
            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(R.string.pref_browser_clear_cookie_dialog_message)
                .setNegativeButton(R.string.dialog_cancel)
                .setPositiveButton(R.string.dialog_ok) {
                    val instance = CookieManager.getInstance()
                    instance.removeAllCookies(null)
                    instance.flush()
                }
                .create()
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_CLEAR_COOKIE)
        }

        // 閲覧履歴削除
        binding.webViewClearHistoryButton.setOnClickListener {
            val dialog = AlertDialogFragment2.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(R.string.pref_browser_clear_history_dialog_message)
                .setNegativeButton(R.string.dialog_cancel)
                .setPositiveButton(R.string.dialog_ok) {
                    // TODO: まず閲覧履歴を実装する
                }
                .create()
            dialog.showAllowingStateLoss(childFragmentManager, DIALOG_CLEAR_HISTORY)
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

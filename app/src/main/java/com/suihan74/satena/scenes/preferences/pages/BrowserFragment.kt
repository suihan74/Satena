package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.scenes.browser.*
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.satena.scenes.preferences.browser.StartPageUrlEditingDialog
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 「ブラウザ」画面
 */
class BrowserFragment : ListPreferencesFragment() {
    override val viewModel by lazy {
        BrowserViewModel(requireContext())
    }
}

// ------ //

class BrowserViewModel(context: Context) : ListPreferencesViewModel(context) {
    lateinit var browserRepo : BrowserRepository  // todo

    lateinit var historyRepo : HistoryRepository  // todo

    // ------ //

    val browserMode get() = browserRepo.browserMode

    val startPage get() = browserRepo.startPage

    val secretModeEnabled get() = browserRepo.privateBrowsingEnabled

    val javascriptEnabled get() = browserRepo.javascriptEnabled

    val userAgent get() = browserRepo.userAgent

    val useUrlBlock get() = browserRepo.useUrlBlocking

    val useBottomAppBar get() = browserRepo.useBottomAppBar

    val webViewTheme get() = browserRepo.webViewTheme

    val isForceDarkSupported get() = browserRepo.isForceDarkSupported

    val isForceDarkStrategySupported get() =  browserRepo.isForceDarkStrategySupported

    val searchEngine get() = browserRepo.searchEngine

    val useMarqueeOnBackStackItems get() = browserRepo.useMarqueeOnBackStackItems

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        val browserActivity = fragment.activity as? BrowserActivity

        if (browserActivity != null) {
            browserRepo = browserActivity.viewModel.browserRepo
            historyRepo = browserActivity.viewModel.historyRepo
        }
        else if (!this::browserRepo.isInitialized) {
            val context = fragment.requireContext()
            browserRepo = BrowserRepository(
                HatenaClient,
                SafeSharedPreferences.create(context),
                SafeSharedPreferences.create(context)
            )
            historyRepo = HistoryRepository(SatenaApplication.instance.browserDao)
        }

        super.onCreateView(fragment)

        val owner = fragment.viewLifecycleOwner

        browserMode.observe(owner, {
            load(fragment)
        })
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun createList(fragment: ListPreferencesFragment): List<PreferencesAdapter.Item> = buildList {
        val fragmentManager = fragment.childFragmentManager

        addSection(R.string.pref_browser_section_browser)
        // アプリ内ブラウザから直接開かれている場合はブラウザ変更できないようにする
        if (fragment.activity is PreferencesActivity) {
            addPrefItem(fragment, browserMode, R.string.pref_browser_mode_desc) {
                openEnumSelectionDialog(
                    BrowserMode.values(),
                    browserMode,
                    R.string.pref_browser_mode_desc,
                    fragmentManager
                )
            }
        }
        addPrefItem(fragment, startPage, R.string.pref_browser_start_page_desc) {
            openStartPageUrlEditingDialog(fragmentManager)
        }

        // CustomTabsIntent使用時には以下の設定は使用できない
        if (browserMode.value == BrowserMode.CUSTOM_TABS_INTENT) {
            return@buildList
        }

        // --- //

        addSection(R.string.pref_browser_section_display)
        addPrefItem(fragment, webViewTheme, R.string.pref_browser_theme_desc) {
            openEnumSelectionDialog(
                WebViewTheme.values(),
                webViewTheme,
                R.string.pref_browser_theme_desc,
                fragmentManager
            )
        }
        addPrefToggleItem(fragment, useBottomAppBar, R.string.pref_browser_use_bottom_app_bar_desc)
        addPrefToggleItem(fragment, useMarqueeOnBackStackItems, R.string.pref_browser_use_marquee_on_back_stack_items_desc)

        // --- //

        addSection(R.string.pref_browser_section_features)
        addPrefToggleItem(fragment, secretModeEnabled, R.string.pref_browser_private_browsing_enabled_desc)
        addPrefToggleItem(fragment, javascriptEnabled, R.string.pref_browser_javascript_enabled_desc)
        addPrefToggleItem(fragment, useUrlBlock, R.string.pref_browser_use_url_blocking_desc)
        addPrefItem(fragment, searchEngine, R.string.pref_browser_search_engine_desc) {
            openSearchEngineSelectionDialog(fragmentManager)
        }
        add(PreferenceEditTextItem(userAgent, R.string.pref_browser_user_agent_desc, R.string.pref_browser_user_agent_hint))

        // --- //

        addSection(R.string.pref_browser_section_optimize)
        addButton(fragment, R.string.pref_browser_clear_cache_desc, textColorId = R.color.clearCache) {
            openClearCacheDialog(fragmentManager)
        }
        addButton(fragment, R.string.pref_browser_clear_cookie_desc, textColorId = R.color.clearCache) {
            openClearCookieDialog(fragmentManager)
        }
        addButton(fragment, R.string.pref_browser_clear_history_desc, textColorId = R.color.clearCache) {
            openClearHistoryDialog(fragmentManager)
        }
    }

    // ------ //

    /**
     * スタートページのURLを設定するダイアログを開く
     */
    private fun openStartPageUrlEditingDialog(fragmentManager: FragmentManager) {
        StartPageUrlEditingDialog.createInstance()
            .show(fragmentManager, null)
    }

    /**
     * 検索エンジンを選択するダイアログを開く
     */
    private fun openSearchEngineSelectionDialog(fragmentManager: FragmentManager) {
        val presets = SearchEngineSetting.Presets.values()
        val labels = presets.map { it.setting.title }
        val checkedItem = presets.indexOfFirst { it.setting == searchEngine.value }

        AlertDialogFragment.Builder()
            .setTitle(R.string.pref_browser_dialog_title_search_engine)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(labels, checkedItem) { _, which ->
                searchEngine.value = presets[which].setting
            }
            .dismissOnClickItem(true)
            .create()
            .show(fragmentManager, null)
    }

    // ------ //

    /** キャッシュを削除する */
    private fun openClearCacheDialog(fragmentManager: FragmentManager) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_browser_clear_cache_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                WebView(SatenaApplication.instance).clearCache(true)
                SatenaApplication.instance.showToast(R.string.msg_browser_removed_all_caches)
            }
            .create()
            .show(fragmentManager, null)
    }

    /** クッキーを削除する */
    private fun openClearCookieDialog(fragmentManager: FragmentManager) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_browser_clear_cookie_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                val instance = CookieManager.getInstance()
                instance.removeAllCookies(null)
                instance.flush()
                SatenaApplication.instance.showToast(R.string.msg_browser_removed_all_cookies)
            }
            .create()
            .show(fragmentManager, null)
    }

    /** 閲覧履歴を削除する */
    private fun openClearHistoryDialog(fragmentManager: FragmentManager) {
        AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_browser_clear_history_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                viewModelScope.launch(Dispatchers.Main) {
                    runCatching {
                        historyRepo.clearHistories()
                    }
                    SatenaApplication.instance.showToast(R.string.msg_browser_removed_all_histories)
                }
            }
            .create()
            .show(fragmentManager, null)
    }
}

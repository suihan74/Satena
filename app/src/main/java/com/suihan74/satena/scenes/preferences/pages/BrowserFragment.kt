package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.TextInputDialogFragment
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.browser.BookmarksListType
import com.suihan74.satena.models.browser.BrowserHistoryLifeSpan
import com.suihan74.satena.scenes.browser.*
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.satena.scenes.preferences.browser.UrlBlockingFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.extensions.observerForOnlyUpdates
import com.suihan74.utilities.extensions.whenFalse
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

    private var browserViewModel : com.suihan74.satena.scenes.browser.BrowserViewModel? = null

    // ------ //

    private val browserMode get() = browserRepo.browserMode

    private val startPage get() = browserRepo.startPage

    private val secretModeEnabled get() = browserRepo.privateBrowsingEnabled

    private val javascriptEnabled get() = browserRepo.javascriptEnabled

    private val userAgent get() = browserRepo.userAgent

    private val useUrlBlock get() = browserRepo.useUrlBlocking

    private val useBottomAppBar get() = browserRepo.useBottomAppBar

    private val webViewTheme get() = browserRepo.webViewTheme

    private val isForceDarkSupported get() = browserRepo.isForceDarkSupported

    private val isForceDarkStrategySupported get() =  browserRepo.isForceDarkStrategySupported

    private val searchEngine get() = browserRepo.searchEngine

    private val useMarqueeOnBackStackItems get() = browserRepo.useMarqueeOnBackStackItems

    private val autoFetchBookmarks get() = browserRepo.autoFetchBookmarks

    private val initialBookmarksList get() = browserRepo.initialBookmarksList

    private val drawerTouchSlopScale get() = browserRepo.drawerTouchSlop

    private val drawerPagerTouchSlopScale get() = browserRepo.drawerPagerScrollSensitivity

    private val historyLifeSpan get() = browserRepo.historyLifeSpan

    // ------ //

    override fun onCreateView(fragment: ListPreferencesFragment) {
        val browserActivity = fragment.activity as? BrowserActivity

        if (browserActivity != null) {
            browserViewModel = browserActivity.viewModel
            browserRepo = browserActivity.viewModel.browserRepo
            historyRepo = browserActivity.viewModel.historyRepo
        }
        else if (!this::browserRepo.isInitialized) {
            val context = fragment.requireContext()
            val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
            val browserSettings = SafeSharedPreferences.create<BrowserSettingsKey>(context)

            browserRepo = BrowserRepository(HatenaClient, prefs, browserSettings)
            historyRepo = HistoryRepository(browserSettings, SatenaApplication.instance.browserDao)
        }

        super.onCreateView(fragment)

        val owner = fragment.viewLifecycleOwner

        browserMode.observe(owner, observerForOnlyUpdates {
            load(fragment)
        })

        historyLifeSpan.observe(owner, observerForOnlyUpdates {
            historyRepo.clearLastRefreshed()
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
            val values = buildList {
                add(WebViewTheme.AUTO)
                add(WebViewTheme.NORMAL)
                if (isForceDarkSupported) {
                    if (isForceDarkStrategySupported) {
                        add(WebViewTheme.DARK)
                    }
                    add(WebViewTheme.FORCE_DARK)
                }
            }
            openEnumSelectionDialog(
                values.toTypedArray(),
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
        addPrefToggleItem(fragment, javascriptEnabled, R.string.pref_browser_javascript_enabled_desc) { value ->
            AlertDialogFragment.Builder()
                .setTitle(R.string.confirm_dialog_title_simple)
                .setMessage(
                    if (value) R.string.browser_menu_javascript_off_desc
                    else R.string.browser_menu_javascript_on_desc
                )
                .setPositiveButton(R.string.dialog_ok) {
                    javascriptEnabled.value = !value
                }
                .setNegativeButton(R.string.dialog_cancel)
                .show(fragmentManager)
        }
        addPrefToggleItem(fragment, useUrlBlock, R.string.pref_browser_use_url_blocking_desc)
        addButton(fragment, R.string.pref_browser_open_url_blocking_desc) {
            fragment.childFragmentManager.beginTransaction()
                .replace(R.id.main_layout, UrlBlockingFragment.createInstance())
                .commit()
        }
        addPrefItem(fragment, searchEngine, R.string.pref_browser_search_engine_desc) {
            openSearchEngineSelectionDialog(fragmentManager)
        }
        addPrefItem(fragment, userAgent, R.string.pref_browser_user_agent_desc) {
            openUserAgentEditingDialog(fragmentManager)
        }

        // --- //

        addSection(R.string.pref_browser_section_behavior)
        addButton(fragment, R.string.pref_browser_drawer_touch_slop_desc) {
            SliderDialog.createInstance(
                titleId = R.string.pref_browser_drawer_touch_slop_desc,
                messageId = R.string.pref_browser_drawer_touch_slop_dialog_message,
                min = 0f,
                max = 1f,
                value = drawerTouchSlopScale.value ?: 1f
            ).setOnCompleteListener { value, _ ->
                drawerTouchSlopScale.value = value
            }.show(fragmentManager, null)
        }
        addButton(fragment, R.string.pref_browser_drawer_pager_touch_slop_desc) {
            SliderDialog.createInstance(
                titleId = R.string.pref_browser_drawer_pager_touch_slop_desc,
                messageId = R.string.pref_pager_scroll_sensitivity_dialog_message,
                min = 0.1f,
                max = 1f,
                value = drawerPagerTouchSlopScale.value ?: 1f
            ).setOnCompleteListener { value, _ ->
                drawerPagerTouchSlopScale.value = value
            }.show(fragmentManager, null)
        }

        // --- //

        addSection(R.string.pref_browser_section_bookmarks)
        addPrefToggleItem(fragment, autoFetchBookmarks, R.string.pref_browser_auto_fetch_bookmarks_desc)
        addPrefItem(fragment, initialBookmarksList, R.string.pref_browser_initial_bookmarks_list_desc) {
            openEnumSelectionDialog(
                BookmarksListType.values(),
                initialBookmarksList,
                R.string.pref_browser_initial_bookmarks_list_desc,
                fragmentManager
            )
        }

        // --- //

        addSection(R.string.pref_browser_section_optimize)
        addPrefItem(fragment, historyLifeSpan, R.string.pref_browser_history_lifespan_desc) {
            openEnumSelectionDialog(
                BrowserHistoryLifeSpan.values(),
                historyLifeSpan,
                R.string.pref_browser_history_lifespan_desc,
                fragmentManager
            )
        }
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
        val browserViewModel = browserViewModel
        val dialog =
            if (browserViewModel != null) {
                TextInputDialogFragment.createInstance(
                    titleId = R.string.pref_browser_start_page_desc,
                    descriptionId = R.string.pref_browser_start_page_message,
                    hintId = R.string.pref_browser_start_page_hint,
                    neutralButtonTextId = R.string.pref_browser_start_page_set_current,
                    initialValue = startPage.value
                ).also {
                    it.setOnClickNeutralButtonListener { _, f ->
                        f.setText(browserViewModel.url.value.orEmpty())
                    }
                }
            }
            else {
                TextInputDialogFragment.createInstance(
                    titleId = R.string.pref_browser_start_page_desc,
                    descriptionId = R.string.pref_browser_start_page_message,
                    hintId = R.string.pref_browser_start_page_hint,
                    initialValue = startPage.value
                )
            }

        dialog.setValidator { url -> URLUtil.isNetworkUrl(url).whenFalse {
            SatenaApplication.instance.showToast(R.string.invalid_url_error)
        } }

        dialog.setOnCompleteListener {
            startPage.value = it
        }

        dialog.show(fragmentManager, null)
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
    /**
     * UserAgentを設定するダイアログを開く
     */
    private fun openUserAgentEditingDialog(fragmentManager: FragmentManager) {
        val dialog = TextInputDialogFragment.createInstance(
            titleId = R.string.pref_browser_user_agent_desc,
            hintId = R.string.pref_browser_user_agent_hint,
            initialValue = userAgent.value
        )

        dialog.setOnCompleteListener {
            userAgent.value = it
        }

        dialog.show(fragmentManager, null)
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

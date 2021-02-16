package com.suihan74.satena.scenes.preferences.pages

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.WebViewTheme
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.preferences.*
import com.suihan74.utilities.SafeSharedPreferences

/**
 * 「ブラウザ」画面
 */
class BrowserViewModel(context: Context) : ListPreferencesViewModel(context) {
    lateinit var browserRepo : BrowserRepository  // todo

    lateinit var historyRepo : HistoryRepository  // todo

    // ------ //

    val browserMode get() = browserRepo.browserMode

    val startPage get() = browserRepo.startPage

    /** 編集中のスタートページURL */
    val startPageEditText get() = MutableLiveData("")

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
        addPrefItem(fragment, startPageEditText, R.string.pref_browser_start_page_desc) {
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
        add(PreferenceEditTextItem(userAgent, R.string.pref_browser_user_agent_desc, R.string.pref_browser_user_agent_hint))
    }
}

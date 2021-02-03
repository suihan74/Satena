package com.suihan74.satena.scenes.preferences.pages

import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.SearchEngineSetting
import com.suihan74.satena.scenes.browser.WebViewTheme
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.utilities.exceptions.InvalidUrlException
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesBrowserViewModel(
    val browserRepo : BrowserRepository,
    val historyRepo : HistoryRepository,
    /** 設定アクティビティで開かれているかどうか */
    val isPreferencesActivity : Boolean
) : ViewModel() {

    private val DIALOG_WEB_VIEW_THEME by lazy { "DIALOG_WEB_VIEW_THEME" }
    private val DIALOG_BROWSER_MODE by lazy { "DIALOG_BROWSER_MODE" }
    private val DIALOG_CLEAR_CACHE by lazy { "DIALOG_CLEAR_CACHE" }
    private val DIALOG_CLEAR_COOKIE by lazy { "DIALOG_CLEAR_COOKIE" }
    private val DIALOG_CLEAR_HISTORY by lazy { "DIALOG_CLEAR_HISTORY" }

    // ------ //

    val browserMode by lazy {
        browserRepo.browserMode
    }

    val startPage by lazy {
        browserRepo.startPage
    }

    /** 編集中のスタートページURL */
    val startPageEditText by lazy {
        MutableLiveData<String>("")
    }

    val secretModeEnabled by lazy {
        browserRepo.privateBrowsingEnabled
    }

    val javascriptEnabled by lazy {
        browserRepo.javascriptEnabled
    }

    val userAgent by lazy {
        browserRepo.userAgent
    }

    val useUrlBlock by lazy {
        browserRepo.useUrlBlocking
    }

    val useBottomAppBar by lazy {
        browserRepo.useBottomAppBar
    }

    val webViewTheme by lazy {
        browserRepo.webViewTheme
    }

    val isForceDarkSupported by lazy {
        browserRepo.isForceDarkSupported
    }

    val isForceDarkStrategySupported by lazy {
        browserRepo.isForceDarkStrategySupported
    }

    val searchEngine by lazy {
        browserRepo.searchEngine
    }

    val useMarqueeOnBackStackItems by lazy {
        browserRepo.useMarqueeOnBackStackItems
    }

    // ------ //

    /**
     * スタートページURLを登録する
     * 
     * @throws InvalidUrlException
     */
    fun registerStartPageUrl() {
        val url = startPageEditText.value ?: ""
        if (!URLUtil.isNetworkUrl(url)) {
            throw InvalidUrlException(url)
        }
        startPage.value = url
    }

    /** WebViewのテーマを指定する */
    @OptIn(ExperimentalStdlibApi::class)
    fun openWebViewThemeSelectionDialog(fragmentManager: FragmentManager) {
        val items = buildList {
            add(WebViewTheme.AUTO)
            add(WebViewTheme.NORMAL)
            if (isForceDarkSupported) {
                if (isForceDarkStrategySupported) {
                    add(WebViewTheme.DARK)
                }
                add(WebViewTheme.FORCE_DARK)
            }
        }
        val labels = items.map { it.textId }
        val checkedItem = webViewTheme.value?.ordinal ?: 0

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_browser_theme_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(labels, checkedItem) { _, which ->
                webViewTheme.value = items[which]
            }
            .dismissOnClickItem(true)
            .create()

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_WEB_VIEW_THEME)
    }

    /** ブラウザモードを選択する */
    fun openBrowserModeSelectionDialog(fragmentManager: FragmentManager) {
        val browserModes = BrowserMode.values()
        val labels = browserModes.map { it.textId }
        val checkedItem = browserModes.indexOf(browserMode.value)

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_browser_mode_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(labels, checkedItem) { _, which ->
                browserMode.value = BrowserMode.fromOrdinal(which)
            }
            .create()

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_BROWSER_MODE)
    }

    /** 検索エンジンを選択する */
    fun openSearchEngineSelectionDialog(fragmentManager: FragmentManager) {
        val presets = SearchEngineSetting.Presets.values()
        val labels = presets.map { it.setting.title }
        val checkedItem = presets.indexOfFirst { it.setting == searchEngine.value }

        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.pref_browser_dialog_title_search_engine)
            .setNegativeButton(R.string.dialog_cancel)
            .setSingleChoiceItems(labels, checkedItem) { _, which ->
                searchEngine.value = presets[which].setting
            }
            .dismissOnClickItem(true)
            .create()
        dialog.showAllowingStateLoss(fragmentManager)
    }

    // ------ //

    /** キャッシュを削除する */
    fun openClearCacheDialog(fragmentManager: FragmentManager) {
        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_browser_clear_cache_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                WebView(SatenaApplication.instance).clearCache(true)
                SatenaApplication.instance.showToast(R.string.msg_browser_removed_all_caches)
            }
            .create()
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_CACHE)
    }

    /** クッキーを削除する */
    fun openClearCookieDialog(fragmentManager: FragmentManager) {
        val dialog = AlertDialogFragment.Builder()
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
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_COOKIE)
    }

    /** 閲覧履歴を削除する */
    fun openClearHistoryDialog(fragmentManager: FragmentManager) {
        val dialog = AlertDialogFragment.Builder()
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
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_HISTORY)
    }
}

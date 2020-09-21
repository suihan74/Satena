package com.suihan74.satena.scenes.preferences.pages

import android.webkit.CookieManager
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.scenes.browser.BrowserMode
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.WebViewTheme
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreferencesBrowserViewModel(
    val repository: BrowserRepository
) : ViewModel() {

    private val DIALOG_WEB_VIEW_THEME by lazy { "DIALOG_WEB_VIEW_THEME" }
    private val DIALOG_BROWSER_MODE by lazy { "DIALOG_BROWSER_MODE" }
    private val DIALOG_CLEAR_CACHE by lazy { "DIALOG_CLEAR_CACHE" }
    private val DIALOG_CLEAR_COOKIE by lazy { "DIALOG_CLEAR_COOKIE" }
    private val DIALOG_CLEAR_HISTORY by lazy { "DIALOG_CLEAR_HISTORY" }

    // ------ //

    /** 設定アクティビティで開かれているかどうか */
    var isPreferencesActivity : Boolean = false

    val browserMode by lazy {
        repository.browserMode
    }

    val startPage by lazy {
        repository.startPage
    }

    val secretModeEnabled by lazy {
        repository.privateBrowsingEnabled
    }

    val javascriptEnabled by lazy {
        repository.javascriptEnabled
    }

    val userAgent by lazy {
        repository.userAgent
    }

    val useUrlBlock by lazy {
        repository.useUrlBlocking
    }

    val useBottomAppBar by lazy {
        repository.useBottomAppBar
    }

    val webViewTheme by lazy {
        repository.webViewTheme
    }

    val isForceDarkSupported by lazy {
        repository.isForceDarkSupported
    }

    val isForceDarkStrategySupported by lazy {
        repository.isForceDarkStrategySupported
    }

    // ------ //

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

        val dialog = AlertDialogFragment2.Builder()
            .setTitle(R.string.pref_browser_theme_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setItems(labels) { _, which ->
                webViewTheme.value = items[which]
            }
            .create()

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_WEB_VIEW_THEME)
    }

    /** ブラウザモードを選択する */
    fun openBrowserModeSelectionDialog(fragmentManager: FragmentManager) {
        val labels = BrowserMode.values().map { it.textId }
        val dialog = AlertDialogFragment2.Builder()
            .setTitle(R.string.pref_browser_mode_desc)
            .setNegativeButton(R.string.dialog_cancel)
            .setItems(labels) { _, which ->
                browserMode.value = BrowserMode.fromOrdinal(which)
            }
            .create()

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_BROWSER_MODE)
    }

    /** キャッシュを削除する */
    fun openClearCacheDialog(fragmentManager: FragmentManager) {
        val dialog = AlertDialogFragment2.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_browser_clear_cache_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                WebView(SatenaApplication.instance).clearCache(true)
            }
            .create()
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_CACHE)
    }

    /** クッキーを削除する */
    fun openClearCookieDialog(fragmentManager: FragmentManager) {
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
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_COOKIE)
    }

    /** 閲覧履歴を削除する */
    fun openClearHistoryDialog(fragmentManager: FragmentManager) {
        val dialog = AlertDialogFragment2.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(R.string.pref_browser_clear_history_dialog_message)
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) { dialog ->
                viewModelScope.launch(Dispatchers.Main) {
                    kotlin.runCatching {
                        repository.clearHistories()
                    }
                    dialog.dismiss()
                }
            }
            .dismissOnClickButton(false)
            .create()
        dialog.showAllowingStateLoss(fragmentManager, DIALOG_CLEAR_HISTORY)
    }
}

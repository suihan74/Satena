package com.suihan74.satena.scenes.browser

import androidx.lifecycle.MutableLiveData
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SingleUpdateMutableLiveData

class BrowserRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val browserSettings: SafeSharedPreferences<BrowserSettingsKey>
) {
    /** テーマ */
    val themeId by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light
    }

    /** サインイン状態 */
    val signedIn: Boolean
        get() = client.signedIn()

    /** スタートページ */
    val startPage : String by lazy {
        browserSettings.getString(BrowserSettingsKey.START_PAGE_URL)!!
    }

    /** UserAgent */
    val userAgent = SingleUpdateMutableLiveData<String?>(
        browserSettings.getString(BrowserSettingsKey.USER_AGENT)
    ).apply {
        observeForever {
            browserSettings.edit {
                put(BrowserSettingsKey.USER_AGENT, it)
            }
        }
    }

    /** 検索エンジン */
    val searchEngine = SingleUpdateMutableLiveData<String>(
        browserSettings.getString(BrowserSettingsKey.SEARCH_ENGINE)
    ).apply {
        observeForever {
            browserSettings.edit {
                put(BrowserSettingsKey.SEARCH_ENGINE, it)
            }
        }
    }

    /** JavaScriptの有効状態 */
    val javascriptEnabled = SingleUpdateMutableLiveData(
        browserSettings.getBoolean(BrowserSettingsKey.JAVASCRIPT_ENABLED)
    ).apply {
        observeForever {
            browserSettings.edit {
                put(BrowserSettingsKey.JAVASCRIPT_ENABLED, it)
            }
        }
    }

    /** URLブロックを使用する */
    val useUrlBlocking = MutableLiveData<Boolean>(
        browserSettings.getBoolean(BrowserSettingsKey.USE_URL_BLOCKING)
    ).apply {
        observeForever {
            browserSettings.edit {
                put(BrowserSettingsKey.USE_URL_BLOCKING, it)
            }
        }
    }

    /** ブロックするURLリスト */
    val blockUrls = MutableLiveData<List<BlockUrlSetting>>(
        browserSettings.get(BrowserSettingsKey.BLOCK_URLS)
    ).apply {
        observeForever {
            browserSettings.edit {
                put(BrowserSettingsKey.BLOCK_URLS, it)
                _blockUrlsRegex = null
            }
        }
    }

    private var _blockUrlsRegex : Regex? = null

    /** ブロックするURL判別用の正規表現 */
    val blockUrlsRegex : Regex
        get() {
            if (_blockUrlsRegex == null) {
                _blockUrlsRegex = blockUrls.value?.regex
            }
            return _blockUrlsRegex ?: Regex("")
        }

    /** ページ中のすべてのリソースURL */
    val resourceUrls = ArrayList<ResourceUrl>()

    // ------ //

    /** サインインを行う */
    suspend fun initialize() {
        accountLoader.signInAccounts(reSignIn = false)
    }

    /** BookmarksEntryを取得 */
    suspend fun getBookmarksEntry(url: String) : BookmarksEntry {
        return client.getBookmarksEntryAsync(url).await()
    }
}

/** リソースURL情報 */
data class ResourceUrl(
    /** 対象URL */
    val url: String,

    /** AdBlock設定によりブロックされた */
    val blocked: Boolean
)

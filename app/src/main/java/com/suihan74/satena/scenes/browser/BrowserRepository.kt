package com.suihan74.satena.scenes.browser

import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.R
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.AccountLoader
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SingleUpdateMutableLiveData

class PreferenceLiveData<PrefT, KeyT, ValueT>(
    prefs: PrefT,
    key: KeyT,
    initializer: ((PrefT)->ValueT)? = null
) : SingleUpdateMutableLiveData<ValueT>(initializer?.invoke(prefs))
        where PrefT: SafeSharedPreferences<KeyT>,
              KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT>
{
    init {
        observeForever {
            prefs.edit {
                put(key, it)
            }
        }
    }
}

class BrowserRepository(
    private val client: HatenaClient,
    private val accountLoader: AccountLoader,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val browserSettings: SafeSharedPreferences<BrowserSettingsKey>
) {
    private fun <ValueT> createBrowserSettingsLiveData(
        key: BrowserSettingsKey,
        initializer: ((SafeSharedPreferences<BrowserSettingsKey>)->ValueT)? = null
    ) = PreferenceLiveData(browserSettings, key, initializer)

    /** テーマ */
    val themeId by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light
    }

    /** サインイン状態 */
    val signedIn : Boolean
        get() = client.signedIn()

    /** サインインしているユーザー名 */
    val userSignedIn : String?
        get() = client.account?.name

    /** スタートページ */
    val startPage =
        createBrowserSettingsLiveData(BrowserSettingsKey.START_PAGE_URL) { p ->
            p.getString(BrowserSettingsKey.START_PAGE_URL)
        }

    /** アプリバーを下部に配置する */
    val useBottomAppBar =
        createBrowserSettingsLiveData(BrowserSettingsKey.USE_BOTTOM_APP_BAR) { p ->
            p.getBoolean(BrowserSettingsKey.USE_BOTTOM_APP_BAR)
        }

    /** UserAgent */
    val userAgent =
        createBrowserSettingsLiveData(BrowserSettingsKey.USER_AGENT) { p ->
            p.getString(BrowserSettingsKey.USER_AGENT)
        }

    /** 検索エンジン */
    val searchEngine =
        createBrowserSettingsLiveData(BrowserSettingsKey.SEARCH_ENGINE) { p ->
            p.getString(BrowserSettingsKey.SEARCH_ENGINE)
        }

    /** JavaScriptの有効状態 */
    val javascriptEnabled =
        createBrowserSettingsLiveData(BrowserSettingsKey.JAVASCRIPT_ENABLED) { p ->
            p.getBoolean(BrowserSettingsKey.JAVASCRIPT_ENABLED)
        }

    /** URLブロックを使用する */
    val useUrlBlocking =
        createBrowserSettingsLiveData(BrowserSettingsKey.USE_URL_BLOCKING) { p ->
            p.getBoolean(BrowserSettingsKey.USE_URL_BLOCKING)
        }

    /** ブロックするURLリスト */
    val blockUrls =
        createBrowserSettingsLiveData(BrowserSettingsKey.BLOCK_URLS) { p ->
            p.get<List<BlockUrlSetting>>(BrowserSettingsKey.BLOCK_URLS)
        }.apply {
            observeForever {
                _blockUrlsRegex = null
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

    /** キーワードのキャッシュ */
    val keywordsCache by lazy {
        HashMap<String, List<Keyword>>()
    }

    // ------ //

    /** サインインを行う */
    suspend fun initialize() {
        accountLoader.signInAccounts(reSignIn = false)
    }

    /** BookmarksEntryを取得 */
    suspend fun getBookmarksEntry(url: String) : BookmarksEntry {
        return client.getBookmarksEntryAsync(url).await()
    }

    /** キーワードを取得 */
    suspend fun getKeyword(word: String) : List<Keyword> {
        return keywordsCache[word] ?: let {
            val value = client.getKeyword(word)
            keywordsCache[word] = value
            value
        }
    }
}

/** リソースURL情報 */
data class ResourceUrl(
    /** 対象URL */
    val url: String,

    /** AdBlock設定によりブロックされた */
    val blocked: Boolean
)

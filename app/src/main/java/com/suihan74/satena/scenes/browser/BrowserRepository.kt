package com.suihan74.satena.scenes.browser

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.webkit.WebViewFeature
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.R
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences

class BrowserRepository(
    private val client: HatenaClient,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val browserSettings: SafeSharedPreferences<BrowserSettingsKey>
) {
    private fun <ValueT> createBrowserSettingsLiveData(
        key: BrowserSettingsKey,
        initializer: (p: SafeSharedPreferences<BrowserSettingsKey>, key: BrowserSettingsKey)->ValueT
    ) = PreferenceLiveData(browserSettings, key, initializer)

    /** 利用する内部ブラウザ */
    val browserMode by lazy {
        MutableLiveData(
            BrowserMode.fromId(prefs.getInt(PreferenceKey.BROWSER_MODE))
        ).apply {
            observeForever {
                prefs.edit {
                    putInt(PreferenceKey.BROWSER_MODE, it.id)
                }
            }
        }
    }

    /** アプリのテーマがダークテーマか */
    val isThemeDark by lazy {
        prefs.getBoolean(PreferenceKey.DARK_THEME)
    }

    /** アプリのテーマID */
    val themeId by lazy {
        if (prefs.getBoolean(PreferenceKey.DARK_THEME)) R.style.AppTheme_Dark
        else R.style.AppTheme_Light
    }

    /** ウェブサイトのテーマ指定 */
    val webViewTheme =
        createBrowserSettingsLiveData(BrowserSettingsKey.THEME) { p, key ->
            p.get<WebViewTheme>(key)
        }

    /** ドロワ位置 */
    val drawerGravity by lazy {
        prefs.getInt(PreferenceKey.DRAWER_GRAVITY)
    }

    /** スタートページ */
    val startPage =
        createBrowserSettingsLiveData(BrowserSettingsKey.START_PAGE_URL) { p, key ->
            p.getString(key)
        }

    /** アプリバーを下部に配置する */
    val useBottomAppBar =
        createBrowserSettingsLiveData(BrowserSettingsKey.USE_BOTTOM_APP_BAR) { p, key ->
            p.getBoolean(key)
        }

    /** UserAgent */
    val userAgent =
        createBrowserSettingsLiveData(BrowserSettingsKey.USER_AGENT) { p, key ->
            p.getString(key)
        }

    /** 検索エンジン */
    val searchEngine =
        createBrowserSettingsLiveData(BrowserSettingsKey.SEARCH_ENGINE) { p, key ->
            p.get<SearchEngineSetting>(key)
        }

    /** シークレットモードの有効状態 */
    val privateBrowsingEnabled =
        createBrowserSettingsLiveData(BrowserSettingsKey.PRIVATE_BROWSING_ENABLED) { p, key ->
            p.getBoolean(key)
        }

    /** JavaScriptの有効状態 */
    val javascriptEnabled =
        createBrowserSettingsLiveData(BrowserSettingsKey.JAVASCRIPT_ENABLED) { p, key ->
            p.getBoolean(key)
        }

    /** 「戻る/進む」履歴項目でマーキーを使用する */
    val useMarqueeOnBackStackItems =
        createBrowserSettingsLiveData(BrowserSettingsKey.USE_MARQUEE_ON_BACK_STACK_ITEMS) { p, key ->
            p.getBoolean(key)
        }

    /** URLブロックを使用する */
    val useUrlBlocking =
        createBrowserSettingsLiveData(BrowserSettingsKey.USE_URL_BLOCKING) { p, key ->
            p.getBoolean(key)
        }

    /** ブロックするURLリスト */
    val blockUrls =
        createBrowserSettingsLiveData(BrowserSettingsKey.BLOCK_URLS) { p, key ->
            p.get<List<BlockUrlSetting>>(key)
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

    val isForceDarkStrategySupported
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)

    val isForceDarkSupported
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)

    // ------ //

    /** キーワードを取得 */
    suspend fun getKeyword(word: String) : List<Keyword> {
        return keywordsCache[word] ?: let {
            val value = client.getKeyword(word)
            keywordsCache[word] = value
            value
        }
    }

    // ------ //

    /** 検索用URLを生成する */
    fun getSearchUrl(keyword: String) : String {
        val encodedKeyword = Uri.encode(keyword)
        val engine = searchEngine.value
            ?: BrowserSettingsKey.SEARCH_ENGINE.defaultValue as SearchEngineSetting
        val queryUrl = engine.query

        return if (queryUrl.contains("%s")) {
            String.format(queryUrl, encodedKeyword)
        }
        else queryUrl + encodedKeyword
    }
}


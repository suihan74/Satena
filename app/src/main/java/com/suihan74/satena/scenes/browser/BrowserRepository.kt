package com.suihan74.satena.scenes.browser

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.webkit.WebViewFeature
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.models.BrowserSettingsKey
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.models.Theme
import com.suihan74.satena.models.browser.BookmarksListType
import com.suihan74.satena.models.browser.BrowserHistoryLifeSpan
import com.suihan74.satena.scenes.preferences.createLiveDataEnum
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.whenTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BrowserRepository(
    private val client: HatenaClient,
    private val prefs: SafeSharedPreferences<PreferenceKey>,
    private val browserSettings: SafeSharedPreferences<BrowserSettingsKey>
) {
    private fun <ValueT> createBrowserSettingsLiveData(
        key: BrowserSettingsKey,
        initializer: (p: SafeSharedPreferences<BrowserSettingsKey>, key: BrowserSettingsKey)->ValueT
    ) = PreferenceLiveData(browserSettings, key, initializer)

    // ------ //

    /** 利用する内部ブラウザ */
    val browserMode = MutableLiveData(
            BrowserMode.fromId(prefs.getInt(PreferenceKey.BROWSER_MODE))
        ).apply {
            observeForever {
                prefs.edit {
                    putInt(PreferenceKey.BROWSER_MODE, it.id)
                }
            }
        }

    /** アプリのテーマがダークテーマか */
    val isThemeDark by lazy {
        when (prefs.getInt(PreferenceKey.THEME)) {
            Theme.DARK.id, Theme.EX_DARK.id -> true
            else -> false
        }
    }

    /** アプリのテーマID */
    val themeId by lazy { Theme.themeId(prefs) }

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

    /** ブクマタブを自動的にロードする */
    val autoFetchBookmarks =
        createBrowserSettingsLiveData(BrowserSettingsKey.AUTO_FETCH_BOOKMARKS) { p, key ->
            p.getBoolean(key)
        }

    /** 初期表示ブクマリスト */
    val initialBookmarksList = createLiveDataEnum(
        browserSettings,
        BrowserSettingsKey.INITIAL_BOOKMARKS_LIST,
        { it.ordinal },
        { i -> BookmarksListType.fromOrdinal(i) }
    )

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

    /** ドロワ開閉のスワイプ感度 */
    val drawerTouchSlop =
        createBrowserSettingsLiveData(BrowserSettingsKey.DRAWER_TOUCH_SLOP_SCALE) { p, key ->
            p.getFloat(key)
        }

    /** ドロワページャのスワイプ感度 */
    val drawerPagerScrollSensitivity =
        createBrowserSettingsLiveData(BrowserSettingsKey.DRAWER_PAGER_SCROLL_SENSITIVITY) { p, key ->
            p.getFloat(key)
        }

    /** 履歴の寿命 */
    val historyLifeSpan = createLiveDataEnum(
        browserSettings,
        BrowserSettingsKey.HISTORY_LIFESPAN,
        { it.days },
        { i -> BrowserHistoryLifeSpan.fromDays(i) }
    )

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

    /** ページのロード完了率 */
    val loadingProgress = MutableLiveData<Int>()

    /** 表示中ページのfavicon */
    val faviconBitmap : LiveData<Bitmap?> = MutableLiveData<Bitmap?>()
    private val _faviconBitmap = faviconBitmap as MutableLiveData<Bitmap?>

    /** faviconの読み込み状態 */
    val faviconLoading : LiveData<Boolean> = MutableLiveData(false)
    private val _faviconLoading = faviconLoading as MutableLiveData<Boolean>

    private val faviconMutex = Mutex()

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

    // ------ //

    /** faviconのロード開始 */
    suspend fun startLoadingFavicon() = withContext(Dispatchers.Main) {
        faviconMutex.withLock {
            _faviconBitmap.value = null
            _faviconLoading.value = true
        }
    }

    /** faviconのロード完了。リポジトリに保持 */
    suspend fun loadFavicon(icon: Bitmap?) : Boolean = withContext(Dispatchers.Main) {
        faviconMutex.withLock {
            faviconLoading.value.whenTrue {
                _faviconLoading.value = false
                _faviconBitmap.value = icon
            }
        }
    }
}

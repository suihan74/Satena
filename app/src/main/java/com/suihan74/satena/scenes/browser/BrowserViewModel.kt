package com.suihan74.satena.scenes.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.*
import android.webkit.*
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.browser.favorites.FavoriteSitesRepository
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.browser.keyword.HatenaKeywordPopup
import com.suihan74.utilities.Listener
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.SingleUpdateMutableLiveData
import com.suihan74.utilities.extensions.addUnique
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.extensions.whenTrue
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.android.synthetic.main.activity_browser.*
import kotlinx.coroutines.*
import kotlin.math.absoluteValue

class BrowserViewModel(
    val browserRepo: BrowserRepository,
    val favoriteSitesRepo: FavoriteSitesRepository,
    val historyRepo: HistoryRepository,
    initialUrl: String?
) : ViewModel() {
    init {
        viewModelScope.launch {
            browserRepo.initialize()
            historyRepo.initialize()
        }
    }

    // ------ //

    private val DIALOG_BLOCK_URL by lazy { "DIALOG_BLOCK_URL" }
    private val DIALOG_CONTEXT_MENU by lazy { "DIALOG_CONTEXT_MENU" }

    // ------ //

    /** アプリのテーマ */
    val themeId : Int
        get() = browserRepo.themeId

    /** Webサイトのテーマ指定 */
    val webViewTheme by lazy {
        browserRepo.webViewTheme
    }

    /** 表示中のページURL */
    val url by lazy {
        val startPage = initialUrl ?: browserRepo.startPage.value!!
        SingleUpdateMutableLiveData(startPage).apply {
            observeForever {
                addressText.value = Uri.decode(it)
                bookmarksEntry.value = null
                isUrlFavorite.value = checkUrlFavorite(it)
                loadBookmarksEntry(it)
            }
        }
    }

    /** 表示中のページタイトル */
    val title by lazy {
        MutableLiveData("")
    }

    /** アドレスバー検索で使用する検索エンジン(仮置き) */
    val searchEngine : String
        get() = browserRepo.searchEngine.value!!

    /** ユーザーエージェント(仮置き) */
    val userAgent by lazy {
        browserRepo.userAgent
    }

    val privateBrowsingEnabled by lazy {
        browserRepo.privateBrowsingEnabled
    }

    /** JavaScriptを有効にする */
    val javaScriptEnabled by lazy {
        browserRepo.javascriptEnabled
    }

    /** URLブロッキングを使用する */
    val useUrlBlocking by lazy {
        browserRepo.useUrlBlocking
    }

    /** アプリバーを画面下部に配置する */
    val useBottomAppBar by lazy {
        browserRepo.useBottomAppBar
    }

    /** アドレスバーの入力内容 */
    val addressText by lazy {
        SingleUpdateMutableLiveData("")
    }

    /** 現在表示中のページで読み込んだすべてのURL */
    val resourceUrls : List<ResourceUrl>
        get() = browserRepo.resourceUrls

    /** お気に入りサイト */
    val favoriteSites by lazy {
        favoriteSitesRepo.sites.also {
            it.observeForever {
                val url = url.value ?: return@observeForever
                isUrlFavorite.value = checkUrlFavorite(url)
            }
        }
    }

    /** 表示中のページがお気に入りに登録されているか */
    val isUrlFavorite by lazy {
        MutableLiveData(false)
    }

    // ------ //

    /** 表示中のページのはてなエントリURL */
    val entryUrl by lazy {
        MutableLiveData<String>("")
    }

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry?>(null)
    }

    /** 閲覧履歴 */
    val histories by lazy {
        historyRepo.histories
    }

    /** ロード完了前にページ遷移した場合にロード処理を中断する */
    private var loadBookmarksEntryTask : Deferred<Unit>? = null
        get() = synchronized(loadBookmarksEntryTaskLock) { field }
        set(value) {
            synchronized(loadBookmarksEntryTaskLock) {
                field = value
            }
        }
    private val loadBookmarksEntryTaskLock = Any()

    /** ローディング状態を通知する */
    val loadingBookmarksEntry by lazy {
        MutableLiveData<Boolean>(false)
    }

    // ------ //

    // ページ読み込み完了時に呼ぶ処理
    private var onPageFinishedListener: Listener<String>? = null

    fun setOnPageFinishedListener(listener: Listener<String>?) {
        onPageFinishedListener = listener
    }

    // ------ //

    /** WebViewの設定 */
    fun initializeWebView(wv: WebView, activity: BrowserActivity) {
        wv.webViewClient = BrowserWebViewClient(activity, this)
        wv.webChromeClient = WebChromeClient()

        wv.settings.domStorageEnabled = true
        wv.settings.useWideViewPort = true
        wv.settings.loadWithOverviewMode = true
        setPrivateBrowsing(wv, privateBrowsingEnabled.value ?: false)

        // セキュリティ保護を利用可能な全てのバージョンでデフォルトで保護を行う
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(wv.settings, true)
        }

        // どういうわけかクリック時にもonLongClickListenerが呼ばれることがあるので、
        // クリックとして処理したかどうかを記憶しておく
        var handledAsClick = false
        var touchMoved = false
        var velocityTracker: VelocityTracker? = null

        // WebView単体ではシングルタップが検知できないので、onTouchListenerで無理矢理シングルタップを検知させる
        // あくまでリンククリックだけを検出したいので、あえてWebViewClientを使用した方法をとっていない
        @Suppress("ClickableViewAccessibility")
        wv.setOnTouchListener { view, motionEvent ->
            when (motionEvent?.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchMoved = false
                    handledAsClick = false
                    velocityTracker?.clear()
                    velocityTracker = velocityTracker ?: VelocityTracker.obtain()
                    velocityTracker?.addMovement(motionEvent)
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.also { vt ->
                        val pointerId = motionEvent.getPointerId(motionEvent.actionIndex)
                        vt.addMovement(motionEvent)
                        vt.computeCurrentVelocity(1000)
                        if (vt.xVelocity.absoluteValue > 100.0f || vt.yVelocity.absoluteValue > 100.0f) {
                            touchMoved = true
                        }
                    }
                    false
                }

                MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    false
                }

                MotionEvent.ACTION_UP -> {
                    velocityTracker?.recycle()
                    velocityTracker = null

                    val duration = motionEvent.eventTime - motionEvent.downTime
                    handledAsClick = duration < 200L
                    if (handledAsClick) {
                        if (touchMoved) return@setOnTouchListener false

                        val hitTestResult = wv.hitTestResult
                        val url = hitTestResult.extra ?: return@setOnTouchListener false
                        val keywordRegex = Regex("""^https?://(anond\.hatelabo|d\.hatena\.ne)\.jp/keyword/(.+)$""")
                        val word = keywordRegex.find(url)?.groupValues?.getOrNull(2)
                        (word != null).whenTrue {
                            viewModelScope.launch {
                                val result = kotlin.runCatching {
                                    browserRepo.getKeyword(word!!)
                                }

                                val response = result.getOrNull()
                                if (response != null) {
                                    openKeywordPopup(response, view, motionEvent.x.toInt(), motionEvent.y.toInt(), activity)
                                }
                            }
                        }
                    }
                    else false
                }

                else -> false
            }
        }

        wv.setOnLongClickListener {
            if (handledAsClick) return@setOnLongClickListener true

            val hitTestResult = wv.hitTestResult
            val url = hitTestResult.extra ?: return@setOnLongClickListener false
            when (hitTestResult.type) {
                // 画像
                WebView.HitTestResult.IMAGE_TYPE -> {
                    Log.i("image", hitTestResult.extra ?: "")
                    true
                }

                // リンク
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    val dialog = AlertDialogFragment2.Builder()
                        .setTitle(Uri.decode(url))
                        .setItems(listOf(
                            R.string.dialog_open,
                            R.string.browser_menu_share,
                            R.string.browser_menu_bookmarks
                        )) { _, which -> when(which) {
                            0 -> goAddress(url)
                            1 -> share(url, activity)
                            2 -> openBookmarksActivity(url, activity)
                        } }
                        .setNegativeButton(R.string.dialog_close)
                        .create()
                    dialog.showAllowingStateLoss(activity.supportFragmentManager, DIALOG_CONTEXT_MENU) { e ->
                        Log.e("error", Log.getStackTraceString(e))
                    }
                    true
                }

                // 画像リンク
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    Log.i("imglink", hitTestResult.extra ?: "")
                    true
                }

                else -> false
            }
        }

        // jsのON/OFF
        wv.settings.javaScriptEnabled = javaScriptEnabled.value ?: true
        javaScriptEnabled.observe(activity) {
            wv.settings.javaScriptEnabled = it
        }

        // UserAgentの設定
        wv.settings.userAgentString = userAgent.value
        userAgent.observe(activity) {
            wv.settings.userAgentString = it
        }

        // テーマの設定
        webViewTheme.observe(activity) {
            val theme =
                when (it) {
                    WebViewTheme.AUTO ->
                        if (browserRepo.isThemeDark) WebViewTheme.DARK
                        else WebViewTheme.NORMAL

                    else -> it
                }

            if (theme == WebViewTheme.DARK && browserRepo.isForceDarkStrategySupported && browserRepo.isForceDarkSupported) {
                WebSettingsCompat.setForceDark(wv.settings, WebSettingsCompat.FORCE_DARK_ON)
                WebSettingsCompat.setForceDarkStrategy(wv.settings, WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
            }
            else if (theme == WebViewTheme.FORCE_DARK && browserRepo.isForceDarkSupported) {
                WebSettingsCompat.setForceDark(wv.settings, WebSettingsCompat.FORCE_DARK_ON)
                if (browserRepo.isForceDarkStrategySupported) {
                    WebSettingsCompat.setForceDarkStrategy(wv.settings, WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY)
                }
            }
            else if (browserRepo.isForceDarkSupported) {
                WebSettingsCompat.setForceDark(wv.settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        }

        var initialPrivateBrowsingEnabled : Boolean? = privateBrowsingEnabled.value
        privateBrowsingEnabled.observe(activity) {
            setPrivateBrowsing(wv, it)
            if (it != initialPrivateBrowsingEnabled) {
                initialPrivateBrowsingEnabled = null
                wv.reload()
            }
        }
    }

    /**
     * プライベートブラウジングを有効化する
     */
    fun setPrivateBrowsing(wv: WebView, enabled: Boolean) {
        val settings = wv.settings
        if (enabled) {
            // cookie
            CookieManager.getInstance().setAcceptCookie(false)
            // cache
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.setAppCacheEnabled(false)
        }
        else {
            // cookie
            CookieManager.getInstance().setAcceptCookie(true)
            // cache
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.setAppCacheEnabled(true)
        }
    }

    /** 状態変化をオプションメニュー項目に通知する */
    fun bindOptionsMenu(owner: LifecycleOwner, context: Context, menu: Menu) {
        val textOn = "ON"
        val textOff = "OFF"
        useUrlBlocking.observe(owner) {
            val state = if (it) textOn else textOff
            val title = context.getText(R.string.pref_browser_use_url_blocking_desc)
            menu.findItem(R.id.adblock)?.title = "$title: $state"
        }
        javaScriptEnabled.observe(owner) {
            val state = if (it) textOn else textOff
            val title = context.getText(R.string.pref_browser_javascript_enabled_desc)
            menu.findItem(R.id.javascript)?.title = "$title: $state"
        }
    }

    /** オプションメニューの処理 */
    fun onOptionsItemSelected(item: MenuItem, activity: BrowserActivity): Boolean = when (item.itemId) {
        R.id.bookmarks -> {
            openBookmarksActivity(url.value!!, activity)
            true
        }

        R.id.share -> {
            share(url.value!!, activity)
            true
        }

        R.id.add_blocking -> {
            openBlockUrlDialog(activity.supportFragmentManager)
            true
        }

        R.id.adblock -> {
            useUrlBlocking.value = useUrlBlocking.value != true
            activity.webview.reload()
            true
        }

        R.id.javascript -> {
            javaScriptEnabled.value = javaScriptEnabled.value != true
            activity.webview.reload()
            true
        }

        R.id.settings -> {
            activity.showPreferencesFragment()
            true
        }

        R.id.exit -> {
            activity.finish()
            true
        }

        else -> false
    }

    /** はてなキーワードの解説ポップアップを開く */
    @OptIn(ExperimentalStdlibApi::class)
    private fun openKeywordPopup(response: List<Keyword>, anchor: View, x: Int, y: Int, activity: BrowserActivity) {
        if (response.isNotEmpty()) {
            // 一般的な用法を優先して表示する
            val generalItem = response.firstOrNull { it.category.contains("一般") }
            val data =
                if (generalItem == null) response
                else  buildList {
                    add(generalItem)
                    addAll(response.minus(generalItem))
                }

            val popup = HatenaKeywordPopup(activity, data)
            popup.showAsDropDown(anchor, x - 100, y - 32, Gravity.TOP)
        }
    }

    /** ブクマ一覧画面を開く */
    private fun openBookmarksActivity(url: String, activity: BrowserActivity) {
        val intent = Intent(activity, BookmarksActivity::class.java).also {
            val bEntry = bookmarksEntry.value
            val eid = bEntry?.id ?: 0L

            if (url == this.url.value && eid > 0L) {
                it.putExtra(BookmarksActivity.EXTRA_ENTRY_ID, eid)
            }
            else {
                it.putExtra(BookmarksActivity.EXTRA_ENTRY_URL, url)
            }
        }
        activity.startActivity(intent)
    }

    /** リンクを共有 */
    private fun share(url: String, activity: BrowserActivity) {
        val intent = Intent().also {
            it.action = Intent.ACTION_SEND
            it.type = "text/plain"
            it.putExtra(Intent.EXTRA_TEXT, url)
        }
        activity.startActivity(intent)
    }

    // ------ //

    /** BookmarksEntryを更新 */
    fun loadBookmarksEntry(url: String, onFinally: OnFinally? = null) {
        loadBookmarksEntryTask?.cancel()
        loadBookmarksEntryTask = viewModelScope.async(Dispatchers.Default) {
            loadingBookmarksEntry.postValue(true)

            // 渡されたページURLをエントリURLに修正する
            val modifyResult = kotlin.runCatching {
                modifySpecificUrls(url)
            }
            val modifiedUrl = modifyResult.getOrNull() ?: url
            entryUrl.postValue(modifiedUrl)

            try {
                bookmarksEntry.postValue(browserRepo.getBookmarksEntry(modifiedUrl))
            }
            catch (e: CancellationException) {
                Log.w("coroutine", "loadBookmarksEntryTask has been canceled")
            }
            catch (e: Throwable) {
                Log.e("loadBookmarksEntry", Log.getStackTraceString(e))
                bookmarksEntry.postValue(null)
            }
            finally {
                onFinally?.invoke()
            }

            loadBookmarksEntryTask = null
            loadingBookmarksEntry.postValue(false)
        }
    }

    /** アドレスバーの入力内容に沿ってページ遷移 */
    fun goAddress(moveToUrl: String? = null) : Boolean {
        moveToUrl?.let { addressText.value = moveToUrl }

        val addr = addressText.value
        return when {
            addr.isNullOrBlank() -> false

            // アドレスが渡されたら直接遷移する
            URLUtil.isValidUrl(addr) -> {
                url.value = addr
                true
            }

            // それ以外は入力内容をキーワードとして検索を行う
            else -> {
                url.value = searchEngine + Uri.encode(addr)
                true
            }
        }
    }

    /** ページ読み込み開始時の処理 */
    fun onPageStarted(url: String) {
        this.title.value = url
        this.url.value = url
        browserRepo.resourceUrls.clear()
    }

    /** ページ読み込み完了時の処理 */
    fun onPageFinished(view: WebView?, url: String) {
        val title = view?.title ?: url
        this.title.value = title
        browserRepo.resourceUrls.addUnique(ResourceUrl(url, false))
        viewModelScope.launch {
            historyRepo.insertHistory(url, title)
        }

        onPageFinishedListener?.invoke(url)
    }

    /** リソースを追加 */
    fun addResource(url: String, blocked: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        browserRepo.resourceUrls.addUnique(ResourceUrl(url, blocked))
    }

    // ------ //

    fun checkUrlFavorite(url: String) : Boolean {
        return favoriteSites.value?.any { s -> s.url == url } ?: false
    }

    /** 表示中のページをお気に入りに登録する */
    fun favoriteCurrentPage() {
        val url = url.value ?: return
        val title = title.value ?: url
        favoriteSitesRepo.favorite(url, title, historyRepo.getFaviconUrl(url))
    }

    /** 表示中のページをお気に入りから除外する */
    fun unfavoriteCurrentPage() {
        val url = url.value ?: return
        val site = favoriteSitesRepo.sites.value?.firstOrNull { it.url == url } ?: return
        favoriteSitesRepo.unfavorite(site)
    }

    // ------ //

    /** 新しいブロック設定を追加するダイアログを開く */
    fun openBlockUrlDialog(
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {
        UrlBlockingDialog.createInstance(resourceUrls).run {
            showAllowingStateLoss(fragmentManager, DIALOG_BLOCK_URL)

            setOnCompleteListener { setting ->
                val blockList = browserRepo.blockUrls.value ?: emptyList()

                if (blockList.none { it.pattern == setting.pattern }) {
                    browserRepo.blockUrls.value = blockList.plus(setting)
                }

                SatenaApplication.instance.showToast(R.string.msg_add_url_blocking_succeeded)
            }
        }
    }
}

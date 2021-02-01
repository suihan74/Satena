package com.suihan74.satena.scenes.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.*
import android.webkit.*
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.suihan74.satena.GlideApp
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.getEntryRootUrl
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.models.browser.HistoryPage
import com.suihan74.satena.scenes.bookmarks.BookmarksActivity
import com.suihan74.satena.scenes.bookmarks.repository.BookmarksRepository
import com.suihan74.satena.scenes.browser.history.HistoryRepository
import com.suihan74.satena.scenes.browser.keyword.HatenaKeywordPopup
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSiteRegistrationDialog
import com.suihan74.satena.scenes.preferences.favoriteSites.FavoriteSitesRepository
import com.suihan74.utilities.Listener
import com.suihan74.utilities.SingleUpdateMutableLiveData
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import java.io.File
import kotlin.math.absoluteValue

class BrowserViewModel(
    val browserRepo: BrowserRepository,
    val bookmarksRepo: BookmarksRepository,
    val favoriteSitesRepo: FavoriteSitesRepository,
    val historyRepo: HistoryRepository,
    private val initialUrl: String?
) : ViewModel() {

    enum class RequestCode {
        /** 画像ファイルの保存先選択画面とのやりとり */
        SAVE_IMAGE
    }

    // ------ //

    /** アプリのテーマ */
    val themeId : Int
        get() = browserRepo.themeId

    /** Webサイトのテーマ指定 */
    private val webViewTheme : LiveData<WebViewTheme> by lazy {
        browserRepo.webViewTheme
    }

    /** ドロワ位置 */
    val drawerGravity by lazy {
        browserRepo.drawerGravity
    }

    /** 表示中のページURL */
    val url = SingleUpdateMutableLiveData<String>().apply {
        observeForever {
            addressText.value = Uri.decode(it)
            bookmarksRepo.bookmarksEntry.value = null
            isUrlFavorite.value = favoriteSitesRepo.contains(it)
            viewModelScope.launch {
                loadBookmarksEntry(it)
            }
        }
    }

    /** 表示中のページタイトル */
    val title = MutableLiveData("")

    /** ユーザーエージェント */
    private val userAgent = browserRepo.userAgent

    private val privateBrowsingEnabled = browserRepo.privateBrowsingEnabled

    /** JavaScriptを有効にする */
    private val javaScriptEnabled = browserRepo.javascriptEnabled

    /** URLブロッキングを使用する */
    val useUrlBlocking = browserRepo.useUrlBlocking

    /** アプリバーを画面下部に配置する */
    val useBottomAppBar = browserRepo.useBottomAppBar

    /** アドレスバーの入力内容 */
    val addressText = SingleUpdateMutableLiveData("")

    /** 「戻る/進む」の履歴 */
    val backStack by lazy { _backStack }
    private val _backStack = MutableLiveData<List<HistoryPage>>()

    /** 「戻る/進む」履歴項目でマーキーを使用する */
    val useMarqueeOnBackStackItems : LiveData<Boolean> =
        browserRepo.useMarqueeOnBackStackItems

    /**
     * ひとつ前に表示していたURL
     *
     * `backStack`の更新時に使用する
     */
    private var previousUrl : String? = null

    /** 現在表示中のページで読み込んだすべてのURL */
    private val resourceUrls : List<ResourceUrl>
        get() = browserRepo.resourceUrls

    /** お気に入りサイト */
    val favoriteSites =
        favoriteSitesRepo.favoriteSites.also {
            it.observeForever {
                val url = url.value ?: return@observeForever
                isUrlFavorite.value = favoriteSitesRepo.contains(url)
            }
        }

    /** 表示中のページがお気に入りに登録されているか */
    val isUrlFavorite = MutableLiveData(false)

    /** ドロワの開閉状態 */
    val drawerOpened = SingleUpdateMutableLiveData<Boolean>()

    /** ボトムシートの開閉状態 */
    val bottomSheetOpened = SingleUpdateMutableLiveData<Boolean>()

    // ------ //

    /** ロード完了前にページ遷移した場合にロード処理を中断する */
    private var loadBookmarksEntryJob : Job? = null

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
    @MainThread
    fun initializeWebView(wv: WebView, activity: BrowserActivity) {
        wv.webViewClient = BrowserWebViewClient(activity, this)
        wv.webChromeClient = WebChromeClient()

        // DOMストレージ使用
        wv.settings.domStorageEnabled = true
        // ページサイズの調整
        wv.settings.useWideViewPort = true
        wv.settings.loadWithOverviewMode = true
        // ズーム
        wv.settings.setSupportZoom(true)
        wv.settings.builtInZoomControls = true
        wv.settings.displayZoomControls = false  // ズームボタンを表示しない
        wv.setInitialScale(100)
        // プライベートブラウジング使用時の設定
        setPrivateBrowsing(wv, privateBrowsingEnabled.value ?: false)

        CookieManager.getInstance().acceptThirdPartyCookies(wv)

        // セキュリティ保護を利用可能な全てのバージョンでデフォルトで保護を行う
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(wv.settings, true)
        }

        // コンテンツのダウンロードに割り込む
        // pdfを開こうとした場合、処理を外部のアプリに投げる
        wv.setDownloadListener { url, _/*userAgent*/, _/*contentDisposition*/, mimeType, _/*size*/ ->
            if (mimeType == "application/pdf") {
                val intent = Intent(Intent.ACTION_VIEW).also {
                    it.setDataAndType(Uri.parse(url), mimeType)
                }
                try {
                    activity.startActivity(intent)
                }
                catch (e: ActivityNotFoundException) {
                    // 開けるアプリが無かったらストアを開く
                    val storeIntent = Intent(Intent.ACTION_VIEW).also {
                        it.data = Uri.parse("market://search?q=pdf")
                    }
                    runCatching {
                        activity.startActivity(storeIntent)
                    }
                }
            }
        }

        // リンククリックに関する挙動を設定
        setLinkClickListener(wv, activity)

        // jsのON/OFF
        wv.settings.javaScriptEnabled = javaScriptEnabled.value ?: true
        javaScriptEnabled.observe(activity, Observer {
            wv.settings.javaScriptEnabled = it
        })

        // UserAgentの設定
        wv.settings.userAgentString = userAgent.value
        userAgent.observe(activity, Observer {
            wv.settings.userAgentString = it
        })

        // テーマの設定
        webViewTheme.observe(activity, Observer {
            val theme =
                when (it) {
                    WebViewTheme.AUTO ->
                        if (browserRepo.isThemeDark) WebViewTheme.DARK
                        else WebViewTheme.NORMAL

                    else -> it
                }

            if (
                theme == WebViewTheme.DARK
                && browserRepo.isForceDarkStrategySupported
                && browserRepo.isForceDarkSupported
            ) {
                WebSettingsCompat.setForceDark(wv.settings, WebSettingsCompat.FORCE_DARK_ON)
                WebSettingsCompat.setForceDarkStrategy(wv.settings, WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
            }
            else if (
                theme == WebViewTheme.FORCE_DARK
                && browserRepo.isForceDarkSupported
            ) {
                WebSettingsCompat.setForceDark(wv.settings, WebSettingsCompat.FORCE_DARK_ON)
                if (browserRepo.isForceDarkStrategySupported) {
                    WebSettingsCompat.setForceDarkStrategy(wv.settings, WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY)
                }
            }
            else if (browserRepo.isForceDarkSupported) {
                WebSettingsCompat.setForceDark(wv.settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
        })

        var initialPrivateBrowsingEnabled : Boolean? = privateBrowsingEnabled.value
        privateBrowsingEnabled.observe(activity, Observer {
            setPrivateBrowsing(wv, it)
            if (it != initialPrivateBrowsingEnabled) {
                initialPrivateBrowsingEnabled = null
                wv.reload()
            }
        })

        // スタートページに遷移
        goAddress(url.value ?: initialUrl ?: browserRepo.startPage.value!!)
    }

    /**
     * リンククリック時の処理を設定する
     */
    private fun setLinkClickListener(wv: WebView, activity: BrowserActivity) {
        // どういうわけかクリック時にもonLongClickListenerが呼ばれることがあるので、
        // クリックとして処理したかどうかを記憶しておく
        var handledAsClick = false
        var touchMoved = false
        var velocityTracker: VelocityTracker? = null

        // WebView単体ではシングルタップが検知できないので、onTouchListenerで無理矢理シングルタップを検知させる
        // あくまでリンククリックだけを検出したいので、あえてWebViewClientを使用した方法をとっていない
        @Suppress("ClickableViewAccessibility", "Recycle")
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
                            val popup = openKeywordPopup(
                                view,
                                motionEvent.x.toInt(),
                                motionEvent.y.toInt(),
                                activity
                            )

                            viewModelScope.launch {
                                val result = runCatching {
                                    browserRepo.getKeyword(word!!)
                                }

                                if (result.isFailure) {
                                    Log.w("keyword", Log.getStackTraceString(result.exceptionOrNull()))
                                }

                                val response = result.getOrDefault(emptyList())
                                popup.setData(response)
                            }
                        }
                    }
                    else false
                }

                else -> false
            }
        }

        // リンク長押しでメニュー表示
        wv.setOnLongClickListener {
            if (handledAsClick) return@setOnLongClickListener true

            val hitTestResult = wv.hitTestResult
            val url = hitTestResult.extra ?: return@setOnLongClickListener false
            when (hitTestResult.type) {
                // 画像
                WebView.HitTestResult.IMAGE_TYPE -> {
                    openImageMenuDialog(url, activity)
                    true
                }

                // リンク
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    openTextLinkMenuDialog(url, activity)
                    true
                }

                // 画像リンク
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    // リンクアドレスは別途入手する必要がある
                    val msg = Handler().obtainMessage()
                    wv.requestFocusNodeHref(msg)

                    // リンクアドレスが取得できたら専用のメニューを開く
                    // 何らかの理由で失敗したら画像用のメニューを開く
                    msg.data.getString("url")?.let { linkUrl ->
                        openImageLinkMenuDialog(linkUrl, url, activity)
                    } ?: openImageMenuDialog(url, activity)

                    true
                }

                else -> false
            }
        }
    }

    // ------ //

    /**
     * テキストリンクに対するメニューを表示する
     */
    private fun openTextLinkMenuDialog(url: String, activity: BrowserActivity) {
        val dialog = AlertDialogFragment.Builder()
            .setTitle(Uri.decode(url))
            .setItems(listOf(
                R.string.browser_link_menu_open_link,
                R.string.browser_link_menu_share_link,
                R.string.browser_link_menu_open_bookmarks,
            )) { _, which -> when(which) {
                0 -> goAddress(url)
                1 -> share(url, activity)
                2 -> openBookmarksActivity(url, activity)
            } }
            .setNegativeButton(R.string.dialog_close)
            .create()

        dialog.showAllowingStateLoss(activity.supportFragmentManager) { e ->
            Log.e("error", Log.getStackTraceString(e))
        }
    }

    /**
     * 画像に対するメニューを表示する
     */
    private fun openImageMenuDialog(url: String, activity: BrowserActivity) {
        val dialog = AlertDialogFragment.Builder()
            .setTitle(Uri.decode(url))
            .setItems(listOf(
                R.string.browser_link_menu_open_image,
                R.string.browser_link_menu_share_image,
                R.string.browser_link_menu_save_image
            )) { _, which -> when(which) {
                0 -> goAddress(url)
                1 -> shareImage(url, activity)
                2 -> publishSaveImageIntent(url, activity)
            } }
            .setNegativeButton(R.string.dialog_close)
            .create()

        dialog.showAllowingStateLoss(activity.supportFragmentManager) { e ->
            Log.e("error", Log.getStackTraceString(e))
        }
    }

    /**
     * 画像リンクに対するメニューを表示する
     */
    private fun openImageLinkMenuDialog(linkUrl: String, imageUrl: String, activity: BrowserActivity) {
        val dialog = AlertDialogFragment.Builder()
            .setTitle(Uri.decode(linkUrl))
            .setItems(listOf(
                R.string.browser_link_menu_open_link,
                R.string.browser_link_menu_share_link,
                R.string.browser_link_menu_open_bookmarks,
                R.string.browser_link_menu_open_image,
                R.string.browser_link_menu_share_image,
                R.string.browser_link_menu_save_image
            )) { _, which -> when(which) {
                0 -> goAddress(linkUrl)
                1 -> share(linkUrl, activity)
                2 -> openBookmarksActivity(linkUrl, activity)
                3 -> goAddress(imageUrl)
                4 -> shareImage(imageUrl, activity)
                5 -> publishSaveImageIntent(imageUrl, activity)
            } }
            .setNegativeButton(R.string.dialog_close)
            .create()

        dialog.showAllowingStateLoss(activity.supportFragmentManager) { e ->
            Log.e("error", Log.getStackTraceString(e))
        }
    }

    // ------ //

    /**
     * プライベートブラウジングを有効化する
     */
    private fun setPrivateBrowsing(wv: WebView, enabled: Boolean) {
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

    // ------ //
    // BrowserActivityに依存する処理をここに丸投げする

    /** 状態変化をオプションメニュー項目に通知する */
    @MainThread
    fun bindOptionsMenu(owner: LifecycleOwner, context: Context, menu: Menu) {
        val textOn = "ON"
        val textOff = "OFF"
        useUrlBlocking.observe(owner, Observer {
            val state = if (it) textOn else textOff
            val title = context.getText(R.string.pref_browser_use_url_blocking_desc)
            menu.findItem(R.id.adblock)?.title = "$title: $state"
        })
        javaScriptEnabled.observe(owner, Observer {
            val state = if (it) textOn else textOff
            val title = context.getText(R.string.pref_browser_javascript_enabled_desc)
            menu.findItem(R.id.javascript)?.title = "$title: $state"
        })
    }

    /** オプションメニューの処理 */
    @MainThread
    fun onOptionsItemSelected(item: MenuItem, activity: BrowserActivity): Boolean = when (item.itemId) {
        R.id.back_stack -> {
            bottomSheetOpened.value = true
            true
        }

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
            activity.webView.reload()
            true
        }

        R.id.javascript -> {
            javaScriptEnabled.value = javaScriptEnabled.value != true
            activity.webView.reload()
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

    /** Intent呼び出し先から結果が返ってきたときの処理 */
    fun onActivityResult(activity: BrowserActivity, requestCode: Int, resultCode: Int, intent: Intent?) {
        when (requestCode) {
            // ファイル保存先の選択
            RequestCode.SAVE_IMAGE.ordinal -> {
                val destUri = intent?.data
                if (resultCode != Activity.RESULT_OK || destUri == null) {
                    Log.d("FilePick", "canceled")
                    return
                }

                saveImage(activity, destUri)
            }
        }
    }

    // ------ //

    /** はてなキーワードの解説ポップアップを開く */
    @OptIn(ExperimentalStdlibApi::class)
    private fun openKeywordPopup(
        anchor: View,
        x: Int,
        y: Int,
        activity: BrowserActivity
    ) : HatenaKeywordPopup {
        return HatenaKeywordPopup(activity).also { popup ->
            popup.showAsDropDown(anchor, x - 100, y - 32, Gravity.TOP)
        }
    }

    /** ブクマ一覧画面を開く */
    private fun openBookmarksActivity(url: String, activity: BrowserActivity) {
        val intent = Intent(activity, BookmarksActivity::class.java).also {
            val bEntry = bookmarksRepo.bookmarksEntry.value
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
        runCatching {
            activity.startActivity(intent)
        }
    }

    /** 画像を共有 */
    private fun shareImage(url: String, activity: BrowserActivity) {
        GlideApp.with(activity)
            .asFile()
            .load(url)
            .into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    val uri = activity.getSharableFileUri(resource)

                    val intent = Intent().also {
                        it.action = Intent.ACTION_SEND
                        it.type = "image/*"
                        it.putExtra(Intent.EXTRA_STREAM, uri)
                    }

                    runCatching {
                        activity.startActivity(intent)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    /** 保存処理中の画像URL */
    private var savingImageUrl : String? = null

    /** 画像保存先の選択画面を開く */
    private fun publishSaveImageIntent(url: String, activity: BrowserActivity) {
        val uri = Uri.parse(url)
        savingImageUrl = url
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).also {
            it.addCategory(Intent.CATEGORY_OPENABLE)
            it.type = "image/*"
            it.putExtra(Intent.EXTRA_TITLE, uri.lastPathSegment ?: uri.path ?: "image")
        }
        activity.startActivityForResult(intent, RequestCode.SAVE_IMAGE.ordinal)
    }

    /** 画像を保存する */
    private fun saveImage(context: Context, destUri: Uri) {
        val srcUrl = savingImageUrl ?: let {
            context.showToast(R.string.browser_save_image_failure)
            return
        }
        savingImageUrl = null

        GlideApp.with(context)
            .asFile()
            .load(srcUrl)
            .into(object : CustomTarget<File>() {
                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    context.contentResolver.openOutputStream(destUri).use { outputStream ->
                        if (outputStream == null) {
                            context.showToast(R.string.browser_save_image_failure)
                            return
                        }
                        resource.inputStream().copyTo(outputStream)
                        context.showToast(R.string.browser_save_image_success)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    // ------ //

    /** BookmarksEntryを更新 */
    private suspend fun loadBookmarksEntry(url: String) = withContext(Dispatchers.Main) {
        loadBookmarksEntryJob?.cancel()

        if (URLUtil.isNetworkUrl(url)) {
            loadingBookmarksEntry.value = true
            loadBookmarksEntryJob = viewModelScope.launch(Dispatchers.Main) {
                runCatching {
                    bookmarksRepo.loadBookmarks(url)
                }
                loadBookmarksEntryJob = null
                loadingBookmarksEntry.value = false
            }
        }
    }

    /** アドレスバーの入力内容に沿ってページ遷移 */
    @MainThread
    fun goAddress(moveToUrl: String? = null) : Boolean {
        val addr = moveToUrl ?: addressText.value
        if (addr.isNullOrBlank()) {
            return false
        }

        drawerOpened.value = false
        bottomSheetOpened.value = false

        when {
            // アドレスが渡されたら直接遷移する
            URLUtil.isValidUrl(addr) -> {
                url.value = addr
            }

            // それ以外は入力内容をキーワードとして検索を行う
            else -> {
                url.value = browserRepo.getSearchUrl(addr)
            }
        }

        return true
    }

    /** ページ読み込み開始時の処理 */
    fun onPageStarted(url: String) {
        this.title.value = url
        this.url.value = url
        browserRepo.resourceUrls.clear()
    }

    /** ページ読み込み完了時の処理 */
    @MainThread
    fun onPageFinished(view: WebView?, url: String) {
        // TODO: タイトルがひとつ前のページから更新されていない場合がある
        val title = view?.title ?: url
        this.title.value = title
        browserRepo.resourceUrls.addUnique(ResourceUrl(url, false))

        val faviconUrl = Uri.parse(url).faviconUrl

        // 通常のwebページだけを履歴に追加する
        if (privateBrowsingEnabled.value != true && URLUtil.isNetworkUrl(url)) {
            viewModelScope.launch {
                historyRepo.insertHistory(url, title, faviconUrl)
            }
        }

        // 「戻る/進む」履歴に追加する
        updateBackStack(url, title, faviconUrl)

        runCatching {
            onPageFinishedListener?.invoke(url)
        }

        previousUrl = url
    }

    /** リソースを追加 */
    fun addResource(url: String, blocked: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        browserRepo.resourceUrls.addUnique(ResourceUrl(url, blocked))
    }

    /** 「戻る/進む」履歴を更新する */
    private fun updateBackStack(url: String, title: String, faviconUrl: String) {
        val histories = _backStack.value.orEmpty()
        if (histories.any { it.url == url }) return

        val previousPagePosition = histories.indexOfFirst { it.url == previousUrl }
        val poppedHistories =
            if (previousPagePosition >= 0 && previousPagePosition + 1 < histories.size) {
                histories.take(previousPagePosition + 1)
            }
            else histories

        _backStack.value = poppedHistories.plus(
            HistoryPage(url, title, faviconUrl, LocalDateTime.now())
        )
    }

    // ------ //

    /** 表示中のページをお気に入りに登録する */
    fun favoriteCurrentPage(fragmentManager: FragmentManager) {
        val url = url.value ?: return
        val title = title.value ?: url
        openFavoritePageDialog(url, title, fragmentManager)
    }

    /** 表示中のページをお気に入りから除外する */
    fun unfavoriteCurrentPage(fragmentManager: FragmentManager) {
        val url = url.value ?: return
        val site = favoriteSitesRepo.favoriteSites.value?.firstOrNull { it.url == url } ?: let {
            SatenaApplication.instance.showToast(R.string.unfavorite_site_failed)
            return
        }
        openUnfavoritePageDialog(site, fragmentManager)
    }

    // ------ //

    /** ページをお気に入りに追加するダイアログを開く */
    private fun openFavoritePageDialog(
        url: String,
        title: String,
        fragmentManager: FragmentManager
    ) {
        val faviconUrl = Uri.parse(url).faviconUrl
        val site = FavoriteSite(url, title, faviconUrl, false)
        val dialog = FavoriteSiteRegistrationDialog.createRegistrationInstance(site)

        dialog.setOnRegisterListener {
            favoriteSitesRepo.favoritePage(url, title, faviconUrl)
        }

        dialog.setDuplicationChecker {
            favoriteSitesRepo.contains(url)
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** ページをお気に入りから除外するか確認するダイアログを開く */
    private fun openUnfavoritePageDialog(
        site: FavoriteSite,
        fragmentManager: FragmentManager
    ) {
        val context = SatenaApplication.instance
        val dialog = AlertDialogFragment.Builder()
            .setTitle(R.string.confirm_dialog_title_simple)
            .setMessage(context.getString(R.string.browser_unfavorite_confirm_msg, site.title))
            .setNegativeButton(R.string.dialog_cancel)
            .setPositiveButton(R.string.dialog_ok) {
                val result = runCatching {
                    favoriteSitesRepo.unfavoritePage(site)
                }

                if (result.isSuccess) {
                    context.showToast(R.string.unfavorite_site_succeeded)
                }
                else {
                    context.showToast(R.string.unfavorite_site_failed)
                    Log.w("unfavorite", Log.getStackTraceString(result.exceptionOrNull()))
                }
            }
            .create()

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 新しいブロック設定を追加するダイアログを開く */
    fun openBlockUrlDialog(
        fragmentManager: FragmentManager
    ) {
        val dialog = UrlBlockingDialog.createInstance(resourceUrls)

        dialog.setOnCompleteListener { setting ->
            val blockList = browserRepo.blockUrls.value ?: emptyList()

            if (blockList.none { it.pattern == setting.pattern }) {
                browserRepo.blockUrls.value = blockList.plus(setting)
            }

            SatenaApplication.instance.showToast(R.string.msg_add_url_blocking_succeeded)
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }

    /** 「戻る/進む」履歴項目のメニューダイアログを開く */
    fun openBackStackItemMenuDialog(
        activity: BrowserActivity,
        page: HistoryPage,
        fragmentManager: FragmentManager
    ) {
        val dialog = BackStackItemMenuDialog.createInstance(page)
        dialog.setOnOpenListener {
            goAddress(page.url)
        }

        dialog.setOnOpenBookmarksListener {
            val intent = Intent(activity, BookmarksActivity::class.java).also {
                it.putExtra(BookmarksActivity.EXTRA_ENTRY_URL, page.url)
            }
            activity.startActivity(intent)
        }

        dialog.setOnOpenEntriesListener {
            viewModelScope.launch(Dispatchers.Main) {
                val intent = Intent(activity, EntriesActivity::class.java).also {
                    val rootUrl = getEntryRootUrl(page.url)
                    it.putExtra(EntriesActivity.EXTRA_SITE_URL, rootUrl)
                }
                activity.startActivity(intent)
            }
        }

        dialog.showAllowingStateLoss(fragmentManager)
    }
}

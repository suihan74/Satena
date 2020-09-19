package com.suihan74.satena.scenes.browser

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.*
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.hatenaLib.BookmarksEntry
import com.suihan74.hatenaLib.Keyword
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment2
import com.suihan74.satena.modifySpecificUrls
import com.suihan74.satena.scenes.bookmarks2.BookmarksActivity
import com.suihan74.satena.scenes.browser.keyword.HatenaKeywordPopup
import com.suihan74.utilities.Listener
import com.suihan74.utilities.SingleUpdateMutableLiveData
import com.suihan74.utilities.extensions.addUnique
import com.suihan74.utilities.extensions.showToast
import com.suihan74.utilities.extensions.whenTrue
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.android.synthetic.main.activity_browser.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class BrowserViewModel(
    val repository: BrowserRepository,
    initialUrl: String?
) : ViewModel() {

    private val DIALOG_BLOCK_URL by lazy { "DIALOG_BLOCK_URL" }
    private val DIALOG_CONTEXT_MENU by lazy { "DIALOG_CONTEXT_MENU" }

    /** テーマ */
    val themeId : Int
        get() = repository.themeId

    /** 表示中のページURL */
    val url by lazy {
        val startPage = initialUrl ?: repository.startPage.value!!
        SingleUpdateMutableLiveData(startPage).apply {
            observeForever {
                addressText.value = Uri.decode(it)
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
        get() = repository.searchEngine.value!!

    /** ユーザーエージェント(仮置き) */
    val userAgent by lazy {
        repository.userAgent
    }

    /** JavaScriptを有効にする */
    val javascriptEnabled by lazy {
        repository.javascriptEnabled
    }

    /** URLブロッキングを使用する */
    val useUrlBlocking by lazy {
        repository.useUrlBlocking
    }

    /** アプリバーを画面下部に配置する */
    val useBottomAppBar by lazy {
        repository.useBottomAppBar
    }

    /** アドレスバーの入力内容 */
    val addressText by lazy {
        SingleUpdateMutableLiveData("")
    }

    /** 現在表示中のページで読み込んだすべてのURL */
    val resourceUrls : List<ResourceUrl>
        get() = repository.resourceUrls

    // ------ //

    /** 表示中のページのはてなエントリURL */
    val entryUrl by lazy {
        MutableLiveData<String>("")
    }

    /** 表示中のページのBookmarksEntry */
    val bookmarksEntry by lazy {
        MutableLiveData<BookmarksEntry?>(null)
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
        wv.webViewClient = BrowserWebViewClient(this)
        wv.webChromeClient = WebChromeClient()

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
                                    repository.getKeyword(word!!)
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
        javascriptEnabled.observe(activity) {
            wv.settings.javaScriptEnabled = it
        }

        // UserAgentの設定
        userAgent.observe(activity) {
            wv.settings.userAgentString = it
        }
    }

    /** 状態変化をオプションメニュー項目に通知する */
    fun bindOptionsMenu(owner: LifecycleOwner, menu: Menu) {
        useUrlBlocking.observe(owner) {
            val state = if (it) "ON" else "OFF"
            menu.findItem(R.id.adblock)?.title = "リソースブロック: $state"
        }
        javascriptEnabled.observe(owner) {
            val state = if (it) "ON" else "OFF"
            menu.findItem(R.id.javascript)?.title = "JavaScript: $state"
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
            javascriptEnabled.value = javascriptEnabled.value != true
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
    private fun loadBookmarksEntry(url: String) = viewModelScope.launch(Dispatchers.Default) {
        // 渡されたページURLをエントリURLに修正する
        val modifyResult = kotlin.runCatching {
            modifySpecificUrls(url)
        }
        val modifiedUrl = modifyResult.getOrNull() ?: url
        entryUrl.postValue(modifiedUrl)

        try {
            bookmarksEntry.postValue(repository.getBookmarksEntry(modifiedUrl))
        }
        catch (e: Throwable) {
            Log.e("loadBookmarksEntry", Log.getStackTraceString(e))
            bookmarksEntry.postValue(null)
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
        repository.resourceUrls.clear()
    }

    /** ページ読み込み完了時の処理 */
    fun onPageFinished(view: WebView?, url: String) {
        this.title.value = view?.title ?: url
        repository.resourceUrls.addUnique(ResourceUrl(url, false))

        onPageFinishedListener?.invoke(url)
    }

    /** リソースを追加 */
    fun addResource(url: String, blocked: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.resourceUrls.addUnique(ResourceUrl(url, blocked))
    }

    // ------ //

    /** 新しいブロック設定を追加するダイアログを開く */
    fun openBlockUrlDialog(
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {
        UrlBlockingDialog.createInstance(resourceUrls).run {
            showAllowingStateLoss(fragmentManager, DIALOG_BLOCK_URL)

            setOnCompleteListener { setting ->
                val blockList = repository.blockUrls.value ?: emptyList()

                if (blockList.none { it.pattern == setting.pattern }) {
                    repository.blockUrls.value = blockList.plus(setting)
                }

                SatenaApplication.instance.showToast(R.string.msg_add_url_blocking_succeeded)
            }
        }
    }
}
